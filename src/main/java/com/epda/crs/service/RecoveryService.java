package com.epda.crs.service;

import com.epda.crs.dao.CourseDAO;
import com.epda.crs.dao.MilestoneDAO;
import com.epda.crs.dao.RecoveryDAO;
import com.epda.crs.dao.RecoveryRecommendationDAO;
import com.epda.crs.dao.ResultDAO;
import com.epda.crs.dao.StudentDAO;
import com.epda.crs.dao.UserDAO;
import com.epda.crs.dto.EligibilityDTO;
import com.epda.crs.enums.MilestoneStatus;
import com.epda.crs.enums.RecoveryStatus;
import com.epda.crs.exception.ValidationException;
import com.epda.crs.model.Course;
import com.epda.crs.model.Milestone;
import com.epda.crs.model.RecoveryPlan;
import com.epda.crs.model.RecoveryRecommendation;
import com.epda.crs.model.Student;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Stateless
public class RecoveryService {

    @Inject private RecoveryDAO               recoveryDAO;
    @Inject private MilestoneDAO              milestoneDAO;
    @Inject private StudentDAO                studentDAO;
    @Inject private CourseDAO                 courseDAO;
    @Inject private RecoveryRecommendationDAO recommendationDAO;
    @Inject private UserDAO                   userDAO;
    @Inject private ResultDAO                 resultDAO;

    @Inject
    private RecoveryRuleService recoveryRuleService;

    @Inject
    private AuditLogService auditLogService;

    @Inject
    private EligibilityService eligibilityService;

    // -----------------------------------------------------------------------
    // Plan CRUD
    // -----------------------------------------------------------------------

    /**
     * Creates a new recovery plan.
     * validateAttempt() is called first — if it throws, nothing is saved.
     * getNextAttemptNumber() is called immediately after to determine scope.
     *
     * @throws ValidationException if IDs are invalid, student/course not found,
     *                             or the 3-attempt limit is exceeded
     */
    public RecoveryPlan createPlan(int studentId, int courseId, int createdBy) {
        if (studentId <= 0) throw new ValidationException("Invalid student ID");
        if (courseId  <= 0) throw new ValidationException("Invalid course ID");

        // Validate attempt limit — throws ValidationException if exceeded (nothing saved yet)
        recoveryRuleService.validateAttempt(studentId, courseId);

        // Determine attempt number and scope after validation passes
        int    attemptNo = recoveryRuleService.getNextAttemptNumber(studentId, courseId);
        String scope     = recoveryRuleService.getAttemptScope(attemptNo);

        // Resolve student and course — throw if either is missing
        Student student = studentDAO.findById((long) studentId)
                .orElseThrow(() -> new ValidationException("Student not found"));
        Course course = courseDAO.findById((long) courseId)
                .orElseThrow(() -> new ValidationException("Course not found"));

        // Build plan
        RecoveryPlan plan = new RecoveryPlan();
        plan.setStudent(student);
        plan.setCourse(course);
        plan.setAttemptNumber(attemptNo);
        plan.setStatus(RecoveryStatus.ACTIVE);
        plan.setRecommendation(scope);
        plan.setStartDate(LocalDate.now());

        // Persist (only reached if validation passed)
        recoveryDAO.save(plan);

        // Audit log
        String actor = userDAO.findById((long) createdBy)
                .map(u -> u.getUsername())
                .orElse("user:" + createdBy);
        if (auditLogService != null) {
            auditLogService.logAction(
                    actor,
                    "CREATE_RECOVERY_PLAN",
                    "RECOVERY_PLAN",
                    plan.getId(),
                    "Recovery plan created for student " + studentId +
                    " in course " + courseId + " (attempt " + attemptNo + ")");
        }

        return plan;
    }

    public void updatePlan(RecoveryPlan plan, String actorUsername) {
        if (plan == null) throw new ValidationException("Recovery plan must not be null");
        recoveryDAO.update(plan);
        auditLogService.logAction(actorUsername, "UPDATE_PLAN", "RECOVERY_PLAN", plan.getId(), "Updated recovery plan details");
    }

    public void updateStatus(int planId, RecoveryStatus status, String actorUsername) {
        if (planId <= 0) throw new ValidationException("Invalid plan ID");
        if (status == null) throw new ValidationException("Status must not be null");
        recoveryDAO.updateStatus((long) planId, status);
        auditLogService.logAction(actorUsername, "UPDATE_PLAN_STATUS", "RECOVERY_PLAN", (long) planId, "Updated plan status to " + status);
    }

    // -----------------------------------------------------------------------
    // Milestone management
    // -----------------------------------------------------------------------

    public void addMilestone(Milestone milestone, String actorUsername) {
        if (milestone == null) throw new ValidationException("Milestone must not be null");
        milestoneDAO.save(milestone);
        auditLogService.logAction(actorUsername, "ADD_MILESTONE", "RECOVERY_MILESTONE", milestone.getId(), "Added milestone to plan " + milestone.getRecoveryPlanId());
    }

    public void updateMilestone(Milestone milestone, String actorUsername) {
        if (milestone == null) throw new ValidationException("Milestone must not be null");
        milestoneDAO.update(milestone);
        auditLogService.logAction(actorUsername, "UPDATE_MILESTONE", "RECOVERY_MILESTONE", milestone.getId(), "Updated milestone details");
    }

    public void updateMilestoneStatus(int milestoneId, MilestoneStatus status, String actorUsername) {
        if (milestoneId <= 0) throw new ValidationException("Invalid milestone ID");
        if (status == null) throw new ValidationException("Status must not be null");
        milestoneDAO.updateStatus((long) milestoneId, status);
        auditLogService.logAction(actorUsername, "UPDATE_MILESTONE_STATUS", "RECOVERY_MILESTONE", (long) milestoneId, "Updated milestone status to " + status);
    }

    // -----------------------------------------------------------------------
    // Grade update
    // -----------------------------------------------------------------------

    /**
     * Updates the grade for a student's recovery attempt and optionally triggers
     * a re-eligibility check. Returns an EligibilityDTO when the grade passes
     * (not F), or null when the grade is still F.
     *
     * @throws ValidationException if planId is invalid or plan not found
     */
    public EligibilityDTO updateStudentGrade(int planId, String newGrade,
                                             double newGradePoint, int updatedBy) {
        if (planId <= 0) throw new ValidationException("Invalid plan ID");
        if (newGrade == null || newGrade.isBlank()) throw new ValidationException("Grade must not be empty");

        RecoveryPlan plan = recoveryDAO.findById((long) planId)
                .orElseThrow(() -> new ValidationException("Recovery plan not found"));

        int studentId  = plan.getStudent().getId().intValue();
        int courseId   = plan.getCourse().getId().intValue();
        int attemptNum = plan.getAttemptNumber();

        // Persist grade change
        resultDAO.updateGrade(studentId, courseId, attemptNum, newGrade, newGradePoint);

        // Auto-complete plan when student passes
        boolean passed = !"F".equalsIgnoreCase(newGrade);
        if (passed) {
            recoveryDAO.updateStatus((long) planId, RecoveryStatus.COMPLETED);
        }

        // Audit log
        String actor = userDAO.findById((long) updatedBy)
                .map(u -> u.getUsername())
                .orElse("user:" + updatedBy);
        if (auditLogService != null) {
            auditLogService.logAction(actor, "UPDATE_GRADE", "RECOVERY_PLAN", (long) planId,
                    "Grade updated to " + newGrade + " for student " + studentId +
                    ", course " + courseId + ", attempt " + attemptNum);
        }

        // Re-eligibility check when student passes
        if (passed) {
            try {
                Student student = plan.getStudent();
                return eligibilityService.checkEligibility(
                        studentId, student.getSemester(), student.getYearOfStudy(), actor);
            } catch (Exception e) {
                // non-fatal — eligibility check failure does not roll back grade update
            }
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // Recommendation management
    // -----------------------------------------------------------------------

    public void addRecommendation(RecoveryRecommendation recommendation, String actorUsername) {
        if (recommendation == null) throw new ValidationException("Recommendation must not be null");
        recommendationDAO.save(recommendation);
auditLogService.logAction(actorUsername, "ADD_RECOMMENDATION", "RECOVERY_RECOMMENDATION", recommendation.getId(), "Added recommendation to plan " + recommendation.getPlanId());    }

    public void updateRecommendation(RecoveryRecommendation recommendation, String actorUsername) {
        if (recommendation == null) throw new ValidationException("Recommendation must not be null");
        recommendationDAO.update(recommendation);
        auditLogService.logAction(actorUsername, "UPDATE_RECOMMENDATION", "RECOVERY_RECOMMENDATION", recommendation.getId(), "Updated recommendation details");
    }

    public void deleteRecommendation(long recId, String actorUsername) {
        if (recId <= 0) throw new ValidationException("Invalid recommendation ID");
        recommendationDAO.delete(recId);
        auditLogService.logAction(actorUsername, "DELETE_RECOMMENDATION", "RECOVERY_RECOMMENDATION", recId, "Deleted recommendation");
    }

    // -----------------------------------------------------------------------
    // Queries
    // -----------------------------------------------------------------------

    public List<RecoveryPlan> findAll() {
        List<RecoveryPlan> plans = recoveryDAO.findAll();
        plans.forEach(p -> p.setMilestones(milestoneDAO.findByRecoveryPlanId(p.getId())));
        return plans;
    }

    public List<RecoveryPlan> findByStudentId(int studentId) {
        if (studentId <= 0) throw new ValidationException("Invalid student ID");
        List<RecoveryPlan> plans = recoveryDAO.findByStudentId(studentId);
        plans.forEach(p -> p.setMilestones(milestoneDAO.findByRecoveryPlanId(p.getId())));
        return plans;
    }

    public Optional<RecoveryPlan> findById(int planId) {
        if (planId <= 0) throw new ValidationException("Invalid plan ID");
        Optional<RecoveryPlan> opt = recoveryDAO.findById((long) planId);
        opt.ifPresent(p -> p.setMilestones(milestoneDAO.findByRecoveryPlanId(p.getId())));
        return opt;
    }

    /** Returns all recommendations attached to a plan. Used by RecoveryBean. */
    public List<RecoveryRecommendation> getRecommendations(int planId) {
        return recommendationDAO.findByPlanId(planId);
    }

    // -----------------------------------------------------------------------
    // Legacy alias (used by RecoveryBean and DashboardService)
    // -----------------------------------------------------------------------

    /** Returns plan by ID directly (null if not found). Used by sendPlanEmail. */
    public RecoveryPlan findPlanById(int planId) {
        if (planId <= 0) return null;
        Optional<RecoveryPlan> opt = recoveryDAO.findById((long) planId);
        opt.ifPresent(p -> p.setMilestones(milestoneDAO.findByRecoveryPlanId(p.getId())));
        return opt.orElse(null);
    }

    /** Alias for findAll() — preserved for backward compatibility. */
    public List<RecoveryPlan> getRecoveryPlans() {
        return findAll();
    }
}
