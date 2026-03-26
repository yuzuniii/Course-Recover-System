package com.epda.crs.bean;

import com.epda.crs.bean.MilestoneBean;
import com.epda.crs.dao.CourseDAO;
import com.epda.crs.dao.ResultDAO;
import com.epda.crs.dao.StudentDAO;
import com.epda.crs.enums.RecoveryStatus;
import com.epda.crs.exception.ValidationException;
import com.epda.crs.model.Course;
import com.epda.crs.model.RecoveryPlan;
import com.epda.crs.model.RecoveryRecommendation;
import com.epda.crs.model.Student;
import com.epda.crs.service.RecoveryService;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import jakarta.inject.Inject;

@Named
@ViewScoped
public class RecoveryBean implements Serializable {

    @Inject
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
    private java.util.List<Student> students;
    private java.util.List<Course> courses;
    private java.util.List<Course> failedCourses;
    private RecoveryRecommendation newRecommendation = new RecoveryRecommendation();
    private java.util.List<RecoveryRecommendation> recommendations = new ArrayList<>();

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @PostConstruct
    public void init() {
        try {
            students = studentDAO.findAll();
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
            recoveryService.createPlan(selectedStudentId, selectedCourseId, actorId);
            loadAllPlans();
            onPlanSelect();
            addInfo("Recovery Plan", "Recovery plan created successfully");
        } catch (ValidationException e) {
            addError("Recovery Plan", e.getMessage());
        } catch (Exception e) {
            addError("Recovery Plan", "An unexpected error occurred");
        }
    }

    public void updatePlanStatus(int planId, RecoveryStatus status) {
        try {
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

    /** Called by p:ajax when the plan dropdown in the milestone section changes. */
    public void onPlanSelect() {
        System.out.println("[RecoveryBean.onPlanSelect] selectedPlanId=" + selectedPlanId);
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
