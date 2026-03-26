package com.epda.crs.service;

import com.epda.crs.dao.CourseDAO;
import com.epda.crs.dao.MilestoneDAO;
import com.epda.crs.dao.RecoveryDAO;
import com.epda.crs.dao.RecoveryRecommendationDAO;
import com.epda.crs.dao.StudentDAO;
import com.epda.crs.dao.UserDAO;
import com.epda.crs.enums.MilestoneStatus;
import com.epda.crs.enums.RecoveryStatus;
import com.epda.crs.exception.ValidationException;
import com.epda.crs.model.Course;
import com.epda.crs.model.Milestone;
import com.epda.crs.model.RecoveryPlan;
import com.epda.crs.model.RecoveryRecommendation;
import com.epda.crs.model.Student;
import com.epda.crs.util.EmailUtil;
import jakarta.ejb.Stateless;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Stateless
public class RecoveryService {

    private final RecoveryDAO               recoveryDAO       = new RecoveryDAO();
    private final MilestoneDAO              milestoneDAO      = new MilestoneDAO();
    private final StudentDAO                studentDAO        = new StudentDAO();
    private final CourseDAO                 courseDAO         = new CourseDAO();
    private final RecoveryRecommendationDAO recommendationDAO = new RecoveryRecommendationDAO();
    private final UserDAO                   userDAO           = new UserDAO();

    @EJB
    private RecoveryRuleService recoveryRuleService;

    @EJB
    private AuditLogService auditLogService;

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

        // Email notification
        EmailUtil.sendEmail(
                student.getStudentNumber() + "@student.crs.local",
                "Recovery Plan Created — " + course.getCourseName(),
                "Dear " + student.getFullName() + ",\n\n" +
                "A recovery plan has been created for " + course.getCourseName() +
                " (attempt " + attemptNo + ").\nScope: " + scope +
                "\nStart date: " + plan.getStartDate());

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

    public void updatePlan(RecoveryPlan plan) {
        if (plan == null) throw new ValidationException("Recovery plan must not be null");
        recoveryDAO.update(plan);
    }

    public void updateStatus(int planId, RecoveryStatus status) {
        if (planId <= 0) throw new ValidationException("Invalid plan ID");
        if (status == null) throw new ValidationException("Status must not be null");
        recoveryDAO.updateStatus((long) planId, status);
    }

    // -----------------------------------------------------------------------
    // Milestone management
    // -----------------------------------------------------------------------

    public void addMilestone(Milestone milestone) {
        if (milestone == null) throw new ValidationException("Milestone must not be null");
        milestoneDAO.save(milestone);
    }

    public void updateMilestone(Milestone milestone) {
        if (milestone == null) throw new ValidationException("Milestone must not be null");
        milestoneDAO.update(milestone);
    }

    public void updateMilestoneStatus(int milestoneId, MilestoneStatus status) {
        if (milestoneId <= 0) throw new ValidationException("Invalid milestone ID");
        if (status == null) throw new ValidationException("Status must not be null");
        milestoneDAO.updateStatus((long) milestoneId, status);
    }

    // -----------------------------------------------------------------------
    // Recommendation management
    // -----------------------------------------------------------------------

    public void addRecommendation(RecoveryRecommendation recommendation) {
        if (recommendation == null) throw new ValidationException("Recommendation must not be null");
        recommendationDAO.save(recommendation);
    }

    public void updateRecommendation(RecoveryRecommendation recommendation) {
        if (recommendation == null) throw new ValidationException("Recommendation must not be null");
        recommendationDAO.update(recommendation);
    }

    public void deleteRecommendation(long recId) {
        if (recId <= 0) throw new ValidationException("Invalid recommendation ID");
        recommendationDAO.delete(recId);
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

    /** Alias for findAll() — preserved for backward compatibility. */
    public List<RecoveryPlan> getRecoveryPlans() {
        return findAll();
    }
}
