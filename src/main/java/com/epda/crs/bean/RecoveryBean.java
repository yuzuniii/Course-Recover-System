package com.epda.crs.bean;

import com.epda.crs.bean.MilestoneBean;
import com.epda.crs.dao.CourseDAO;
import com.epda.crs.dao.ResultDAO;
import com.epda.crs.dao.StudentDAO;
import com.epda.crs.dto.EligibilityDTO;
import com.epda.crs.enums.RecoveryStatus;
import com.epda.crs.exception.ValidationException;
import com.epda.crs.model.Course;
import com.epda.crs.model.RecoveryPlan;
import com.epda.crs.model.RecoveryRecommendation;
import com.epda.crs.model.Student;
import com.epda.crs.service.RecoveryService;
import com.epda.crs.util.EmailUtil;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import jakarta.inject.Inject;
import com.epda.crs.model.Milestone;

@Named
@ViewScoped
public class RecoveryBean implements Serializable {

    @EJB
    private RecoveryService recoveryService;

    @Inject
    private StudentDAO studentDAO;

    @Inject
    private CourseDAO courseDAO;

    @Inject
    private ResultDAO resultDAO;

    @Inject
    private LoginBean loginBean;

    private java.util.List<RecoveryPlan> recoveryPlans;
    private RecoveryPlan selectedPlan;
    private int selectedStudentId;
    private int selectedCourseId;
    private int selectedPlanId;
    private java.util.List<Student> students;
    private java.util.List<Course> courses;
    private java.util.List<Course> failedCourses;
    private RecoveryRecommendation newRecommendation = new RecoveryRecommendation();
    private java.util.List<RecoveryRecommendation> recommendations = new ArrayList<>();
    private RecoveryRecommendation editingRecommendation;
    private Milestone newMilestone = new Milestone();
    private String newGrade;
    private double newGradePoint;
    private int gradingPlanId;
    private RecoveryPlan gradingPlan;

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @PostConstruct
    public void init() {
        try {
            students = studentDAO.findStudentsWithFailures();
        } catch (Exception e) {
            students = new ArrayList<>();
        }
        try {
            courses = courseDAO.findAll();
        } catch (Exception e) {
            courses = new ArrayList<>();
        }
        // If arriving from eligibility page with a pre-selected student,
        // set the dropdown value and load that student's plans automatically.
        String studentParam = FacesContext.getCurrentInstance()
                .getExternalContext()
                .getRequestParameterMap()
                .get("studentId");
        if (studentParam != null && !studentParam.isEmpty()) {
            try {
                selectedStudentId = Integer.parseInt(studentParam);
            } catch (NumberFormatException ignored) {}
        }

        if (selectedStudentId > 0) {
            loadFailedCourses(selectedStudentId);
            loadPlansByStudent();
        } else {
            // Default: show all courses when no student is pre-selected
            failedCourses = courses;
            loadAllPlans();
        }
    }

    // -----------------------------------------------------------------------
    // Actions
    // -----------------------------------------------------------------------

    public void loadAllPlans() {
        try {
            recoveryPlans = recoveryService.findAll();
        } catch (ValidationException e) {
            addError("Load Plans", e.getMessage());
        } catch (Exception e) {
            addError("Load Plans", "An unexpected error occurred");
        }
    }

    public void loadPlansByStudent() {
        try {
            recoveryPlans = recoveryService.findByStudentId(selectedStudentId);
        } catch (ValidationException e) {
            addError("Load Plans", e.getMessage());
        } catch (Exception e) {
            addError("Load Plans", "An unexpected error occurred");
        }
    }

    public void createPlan() {
        try {
            int actorId = (loginBean.getCurrentUser() != null) ? loginBean.getCurrentUser().getId().intValue() : 0;
            RecoveryPlan newPlan = recoveryService.createPlan(selectedStudentId, selectedCourseId, actorId);
            selectedPlanId = newPlan.getId().intValue();
            loadPlansByStudent();
            onPlanSelect();
            addInfo("Recovery Plan", "Recovery plan created. Select the plan and add milestones to track the student's progress.");
        } catch (ValidationException e) {
            addError("Recovery Plan", e.getMessage());
        } catch (Exception e) {
            addError("Recovery Plan", "An unexpected error occurred");
        }
    }

    public void updatePlanStatus(int planId, RecoveryStatus status) {
        try {
            // FIX 6: Block completion if any milestones are still pending
            if (status == RecoveryStatus.COMPLETED && !allMilestonesCompleted(planId)) {
                int pending = 0;
                if (recoveryPlans != null) {
                    for (RecoveryPlan p : recoveryPlans) {
                        if (p.getId() != null && p.getId().intValue() == planId) {
                            List<com.epda.crs.model.Milestone> ms = p.getMilestones();
                            if (ms != null) {
                                for (com.epda.crs.model.Milestone m : ms) {
                                    if (m.getStatus() == null) { pending++; }
                                    else {
                                        String s = m.getStatus().name();
                                        if (!s.equals("DONE") && !s.equals("COMPLETED")) pending++;
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
                addError("Recovery Plan", "Cannot complete plan. " + pending + " milestone(s) are still pending.");
                return;
            }
            String actor = (loginBean.getCurrentUser() != null) ? loginBean.getCurrentUser().getUsername() : "system";
            recoveryService.updateStatus(planId, status, actor);
            loadAllPlans();
            addInfo("Recovery Plan", "Plan status updated successfully");
        } catch (ValidationException e) {
            addError("Recovery Plan", e.getMessage());
        } catch (Exception e) {
            addError("Recovery Plan", "An unexpected error occurred");
        }
    }

    public void addRecommendation() {
        if (selectedPlan == null) return;
        try {
            String actor = (loginBean.getCurrentUser() != null) ? loginBean.getCurrentUser().getUsername() : "system";
            newRecommendation.setPlanId(selectedPlan.getId());
            recoveryService.addRecommendation(newRecommendation, actor);
            newRecommendation = new RecoveryRecommendation();
            refreshRecommendations();
            addInfo("Recommendation", "Recommendation added successfully");
        } catch (ValidationException e) {
            addError("Recommendation", e.getMessage());
        } catch (Exception e) {
            addError("Recommendation", "An unexpected error occurred");
        }
    }

    public void deleteRecommendation(long recId) {
        if (selectedPlan == null) return;
        try {
            String actor = (loginBean.getCurrentUser() != null) ? loginBean.getCurrentUser().getUsername() : "system";
            recoveryService.deleteRecommendation(recId, actor);
            refreshRecommendations();
            addInfo("Recommendation", "Recommendation deleted successfully");
        } catch (ValidationException e) {
            addError("Recommendation", e.getMessage());
        } catch (Exception e) {
            addError("Recommendation", "An unexpected error occurred");
        }
    }

    public void sendPlanEmail(int planId) {
        try {
            recoveryService.findById(planId).ifPresent(plan -> {
                EmailUtil.sendEmail(
                        plan.getStudent().getStudentNumber() + "@student.crs.local",
                        "Recovery Plan Created — " + plan.getCourse().getCourseName(),
                        "Dear " + plan.getStudent().getFullName() + ",\n\n" +
                        "A recovery plan has been created for " + plan.getCourse().getCourseName() +
                        " (attempt " + plan.getAttemptNumber() + ").\nStart date: " + plan.getStartDate());
                addInfo("Email", "Plan notification sent to student.");
            });
        } catch (ValidationException e) {
            addError("Email", e.getMessage());
        } catch (Exception e) {
            addError("Email", "Failed to send email: " + e.getMessage());
        }
    }

    public void prepareEditRecommendation(RecoveryRecommendation rec) {
        editingRecommendation = rec;
        newRecommendation = new RecoveryRecommendation();
        newRecommendation.setRecommendation(rec.getRecommendation());
    }

    public void updateRecommendation() {
        if (editingRecommendation == null) return;
        try {
            String actor = (loginBean.getCurrentUser() != null) ? loginBean.getCurrentUser().getUsername() : "system";
            editingRecommendation.setRecommendation(newRecommendation.getRecommendation());
            recoveryService.updateRecommendation(editingRecommendation, actor);
            editingRecommendation = null;
            newRecommendation = new RecoveryRecommendation();
            refreshRecommendations();
            addInfo("Recommendation", "Recommendation updated successfully");
        } catch (ValidationException e) {
            addError("Recommendation", e.getMessage());
        } catch (Exception e) {
            addError("Recommendation", "An unexpected error occurred");
        }
    }

    public void cancelEditRecommendation() {
        editingRecommendation = null;
        newRecommendation = new RecoveryRecommendation();
    }

    // -----------------------------------------------------------------------
    // Milestone management (FIX 1)
    // -----------------------------------------------------------------------

    /** Adds a milestone to the currently selected plan (ViewScoped — selectedPlanId is retained). */
    public void addMilestone() {
        if (selectedPlanId <= 0) {
            addError("Milestone", "Please select a recovery plan first");
            return;
        }
        try {
            String actor = (loginBean.getCurrentUser() != null) ? loginBean.getCurrentUser().getUsername() : "system";
            newMilestone.setRecoveryPlanId((long) selectedPlanId);
            recoveryService.addMilestone(newMilestone, actor);
            newMilestone = new Milestone();
            // Reload milestones in the RequestScoped MilestoneBean
            MilestoneBean mb = FacesContext.getCurrentInstance().getApplication()
                    .evaluateExpressionGet(FacesContext.getCurrentInstance(), "#{milestoneBean}", MilestoneBean.class);
            if (mb != null) {
                mb.setSelectedPlanId(selectedPlanId);
                mb.loadMilestones();
            }
            addInfo("Milestone", "Milestone added. Keep adding milestones to track the student's progress.");
        } catch (ValidationException e) {
            addError("Milestone", e.getMessage());
        } catch (Exception e) {
            addError("Milestone", "An unexpected error occurred");
        }
    }

    /**
     * FIX 2: Returns true only if the plan has at least one milestone
     * and all milestones are in DONE or COMPLETED status.
     * Uses the already-loaded recoveryPlans list to avoid extra DB calls.
     */
    public boolean allMilestonesCompleted(int planId) {
        if (recoveryPlans == null) return false;
        for (RecoveryPlan p : recoveryPlans) {
            if (p.getId() != null && p.getId().intValue() == planId) {
                List<Milestone> ms = p.getMilestones();
                if (ms == null || ms.isEmpty()) return false;
                for (Milestone m : ms) {
                    if (m.getStatus() == null) return false;
                    String s = m.getStatus().name();
                    if (!s.equals("DONE") && !s.equals("COMPLETED")) return false;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * FIX 3: Selects a plan globally from the plans table row button.
     * Sets selectedPlanId and selectedPlan, loads recommendations and milestones.
     */
    public void selectPlan(int planId) {
        selectedPlanId = planId;
        selectedPlan = null;
        if (recoveryPlans != null) {
            for (RecoveryPlan p : recoveryPlans) {
                if (p.getId() != null && p.getId().intValue() == planId) {
                    selectedPlan = p;
                    break;
                }
            }
        }
        refreshRecommendations();
        MilestoneBean mb = FacesContext.getCurrentInstance().getApplication()
                .evaluateExpressionGet(FacesContext.getCurrentInstance(), "#{milestoneBean}", MilestoneBean.class);
        if (mb != null) {
            mb.setSelectedPlanId(selectedPlanId);
            mb.loadMilestones();
        }
    }

    // -----------------------------------------------------------------------
    // Grade update
    // -----------------------------------------------------------------------

    /** Opens the grade update dialog for the given plan. */
    public void prepareGradeUpdate(RecoveryPlan plan) {
        gradingPlan   = plan;
        gradingPlanId = plan.getId().intValue();
        newGrade      = "";
        newGradePoint = 0.0;
    }

    /**
     * Computes grade point from the grade string.
     * A=4.0, A-=3.7, B+=3.3, B=3.0, B-=2.7, C+=2.3, C=2.0, C-=1.7, F=0.0
     */
    private double gradeToPoint(String grade) {
        return switch (grade) {
            case "A"  -> 4.0;
            case "A-" -> 3.7;
            case "B+" -> 3.3;
            case "B"  -> 3.0;
            case "B-" -> 2.7;
            case "C+" -> 2.3;
            case "C"  -> 2.0;
            case "C-" -> 1.7;
            default   -> 0.0;
        };
    }

    /** Validates the selected grade, calls the service to persist it, and shows the result. */
    public void submitGradeUpdate() {
        if (gradingPlan == null) {
            addError("Grade Update", "No plan selected");
            return;
        }
        if (newGrade == null || newGrade.isBlank()) {
            addError("Grade Update", "Please select a grade");
            return;
        }
        try {
            newGradePoint = gradeToPoint(newGrade);
            int actorId = (loginBean.getCurrentUser() != null)
                    ? loginBean.getCurrentUser().getId().intValue() : 0;

            EligibilityDTO eligibility = recoveryService.updateStudentGrade(
                    gradingPlanId, newGrade, newGradePoint, actorId);

            boolean passed = !"F".equalsIgnoreCase(newGrade);
            if (passed) {
                addInfo("Grade Updated",
                        "Grade updated. Student has passed this course on Attempt " +
                        gradingPlan.getAttemptNumber() + ".");
                if (eligibility != null) {
                    if (eligibility.isEligible()) {
                        addInfo("Eligibility", "Student is now eligible for progression.");
                    } else {
                        FacesContext.getCurrentInstance().addMessage(null,
                                new FacesMessage(FacesMessage.SEVERITY_WARN, "Eligibility",
                                "Student is not yet fully eligible: " + eligibility.getReason()));
                    }
                }
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Grade Updated",
                        "Grade updated. Student still needs recovery. Consider creating Attempt " +
                        (gradingPlan.getAttemptNumber() + 1) + "."));
            }
            gradingPlan   = null;
            gradingPlanId = 0;
            loadAllPlans();
        } catch (ValidationException e) {
            addError("Grade Update", e.getMessage());
        } catch (Exception e) {
            addError("Grade Update", "An unexpected error occurred");
        }
    }

    /** Called by p:ajax when the plan dropdown in the milestone section changes. */
    public void onPlanSelect() {
        System.out.println("[RecoveryBean.onPlanSelect] selectedPlanId=" + selectedPlanId);
        // Find and set the selectedPlan object so recommendations section becomes visible
        selectedPlan = null;
        if (recoveryPlans != null) {
            for (RecoveryPlan plan : recoveryPlans) {
                if (plan.getId() != null && plan.getId().intValue() == selectedPlanId) {
                    selectedPlan = plan;
                    break;
                }
            }
        }
        refreshRecommendations();
        // Also load milestones via MilestoneBean (RequestScoped — looked up via EL)
        MilestoneBean mb = FacesContext.getCurrentInstance()
                .getApplication()
                .evaluateExpressionGet(
                        FacesContext.getCurrentInstance(),
                        "#{milestoneBean}",
                        MilestoneBean.class);
        if (mb != null) {
            mb.setSelectedPlanId(selectedPlanId);
            mb.loadMilestones();
            System.out.println("[RecoveryBean.onPlanSelect] milestones loaded, count=" +
                    (mb.getMilestones() != null ? mb.getMilestones().size() : "null"));
        }
    }

    /** Called by p:ajax when the student dropdown changes. */
    public void onStudentChange() {
        loadFailedCourses(selectedStudentId);
        selectedCourseId = 0;
    }

    /**
     * Loads failed courses for the given student from ResultDAO + CourseDAO.
     * Falls back to all courses when studentId is 0 or no failures are found.
     */
    public void loadFailedCourses(int studentId) {
        if (studentId <= 0) {
            failedCourses = courses;
            return;
        }
        try {
            java.util.List<Integer> ids = resultDAO.findFailedCourseIds(studentId);
            java.util.List<Course> failed = new ArrayList<>();
            for (int id : ids) {
                courseDAO.findById((long) id).ifPresent(failed::add);
            }
            failedCourses = failed.isEmpty() ? courses : failed;
        } catch (Exception e) {
            failedCourses = courses;
        }
    }

    public void prepareNewPlan() {
        selectedStudentId = 0;
        selectedCourseId  = 0;
        newRecommendation = new RecoveryRecommendation();
    }

    /**
     * Called from the eligibility result panel via f:param studentId.
     * Carries the student ID into the redirect URL so init() can pre-select it.
     */
    public String prepareFromEligibility() {
        String param = FacesContext.getCurrentInstance()
                .getExternalContext()
                .getRequestParameterMap()
                .get("studentId");
        if (param != null && !param.isEmpty()) {
            try {
                selectedStudentId = Integer.parseInt(param);
            } catch (NumberFormatException ignored) {}
            return "/pages/recovery.xhtml?faces-redirect=true&studentId=" + param;
        }
        return "/pages/recovery.xhtml?faces-redirect=true";
    }

    // -----------------------------------------------------------------------
    // Getters and setters
    // -----------------------------------------------------------------------

    public java.util.List<RecoveryPlan> getRecoveryPlans() { return recoveryPlans; }
    public void setRecoveryPlans(java.util.List<RecoveryPlan> recoveryPlans) { this.recoveryPlans = recoveryPlans; }

    public RecoveryPlan getSelectedPlan() { return selectedPlan; }
    public void setSelectedPlan(RecoveryPlan selectedPlan) {
        this.selectedPlan = selectedPlan;
        refreshRecommendations();
    }

    public int getSelectedStudentId() { return selectedStudentId; }
    public void setSelectedStudentId(int selectedStudentId) { this.selectedStudentId = selectedStudentId; }

    public int getSelectedCourseId() { return selectedCourseId; }
    public void setSelectedCourseId(int selectedCourseId) { this.selectedCourseId = selectedCourseId; }

    public int getSelectedPlanId() { return selectedPlanId; }
    public void setSelectedPlanId(int selectedPlanId) { this.selectedPlanId = selectedPlanId; }

    public java.util.List<Student> getStudents() { return students; }
    public void setStudents(java.util.List<Student> students) { this.students = students; }

    public java.util.List<Course> getCourses() { return courses; }
    public void setCourses(java.util.List<Course> courses) { this.courses = courses; }

    public java.util.List<Course> getFailedCourses() { return failedCourses; }
    public void setFailedCourses(java.util.List<Course> failedCourses) { this.failedCourses = failedCourses; }

    public RecoveryRecommendation getNewRecommendation() { return newRecommendation; }
    public void setNewRecommendation(RecoveryRecommendation newRecommendation) { this.newRecommendation = newRecommendation; }

    public java.util.List<RecoveryRecommendation> getRecommendations() { return recommendations; }
    public void setRecommendations(java.util.List<RecoveryRecommendation> recommendations) { this.recommendations = recommendations; }

    public RecoveryRecommendation getEditingRecommendation() { return editingRecommendation; }
    public void setEditingRecommendation(RecoveryRecommendation r) { this.editingRecommendation = r; }

    public Milestone getNewMilestone() { return newMilestone; }
    public void setNewMilestone(Milestone newMilestone) { this.newMilestone = newMilestone; }

    public String getNewGrade() { return newGrade; }
    public void setNewGrade(String newGrade) { this.newGrade = newGrade; }

    public double getNewGradePoint() { return newGradePoint; }
    public void setNewGradePoint(double newGradePoint) { this.newGradePoint = newGradePoint; }

    public int getGradingPlanId() { return gradingPlanId; }
    public void setGradingPlanId(int gradingPlanId) { this.gradingPlanId = gradingPlanId; }

    public RecoveryPlan getGradingPlan() { return gradingPlan; }
    public void setGradingPlan(RecoveryPlan gradingPlan) { this.gradingPlan = gradingPlan; }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void refreshRecommendations() {
        if (selectedPlan != null) {
            try {
                recommendations = recoveryService.getRecommendations(
                        selectedPlan.getId().intValue());
            } catch (Exception e) {
                recommendations = new ArrayList<>();
            }
        } else {
            recommendations = new ArrayList<>();
        }
    }

    private void addInfo(String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, summary, detail));
    }

    private void addError(String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, detail));
    }
}
