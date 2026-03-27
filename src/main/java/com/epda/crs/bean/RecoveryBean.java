package com.epda.crs.bean;

import com.epda.crs.dao.CourseDAO;
import com.epda.crs.dao.ResultDAO;
import com.epda.crs.dao.StudentDAO;
import com.epda.crs.dto.EligibilityDTO;
import com.epda.crs.enums.MilestoneStatus;
import com.epda.crs.exception.ValidationException;
import com.epda.crs.model.Course;
import com.epda.crs.model.Milestone;
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
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.primefaces.event.SelectEvent;

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

    private List<RecoveryPlan> recoveryPlans = new ArrayList<>();
    private RecoveryPlan selectedPlan;
    private int selectedStudentId;
    private int selectedCourseId;
    private int selectedPlanId;
    private List<Student> students = new ArrayList<>();
    private List<Course> courses = new ArrayList<>();
    private List<Course> failedCourses = new ArrayList<>();
    private String failedCoursesMessage;
    private RecoveryRecommendation newRecommendation = new RecoveryRecommendation();
    private List<RecoveryRecommendation> recommendations = new ArrayList<>();
    private List<Milestone> milestones = new ArrayList<>();
    private Long editingRecommendationId;
    private String editingRecommendationText = "";
    private Milestone newMilestone = new Milestone();
    private String newGrade;
    private double newGradePoint;
    private int gradingPlanId;
    private RecoveryPlan gradingPlan;

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

        try {
            String studentParam = FacesContext.getCurrentInstance()
                    .getExternalContext()
                    .getRequestParameterMap()
                    .get("studentId");
            if (studentParam != null && !studentParam.isEmpty()) {
                selectedStudentId = Integer.parseInt(studentParam);
            }

            if (selectedStudentId > 0) {
                loadFailedCourses(selectedStudentId);
                loadPlansByStudentInternal(true);
                autoSelectFirstPlan();
            } else {
                failedCoursesMessage = "Select a student to load failed courses.";
                loadAllPlansInternal(false);
            }
        } catch (Exception e) {
            failedCourses = new ArrayList<>();
            failedCoursesMessage = "Select a student to load failed courses.";
            loadAllPlansInternal(false);
        }
    }

    public void loadAllPlans() {
        try {
            loadAllPlansInternal(false);
        } catch (ValidationException e) {
            addError("Load Plans", e.getMessage());
        } catch (Exception e) {
            addError("Load Plans", "An unexpected error occurred");
        }
    }

    public void loadPlansByStudent() {
        try {
            if (selectedStudentId <= 0) {
                addError("Load Plans", "Select a student to filter plans.");
                return;
            }
            loadPlansByStudentInternal(false);
        } catch (ValidationException e) {
            addError("Load Plans", e.getMessage());
        } catch (Exception e) {
            addError("Load Plans", "An unexpected error occurred");
        }
    }

    public void createPlan() {
        try {
            if (selectedStudentId <= 0) {
                addError("Recovery Plan", "Please select a student first.");
                return;
            }
            if (selectedCourseId <= 0) {
                addError("Recovery Plan", "Please select a failed course first.");
                return;
            }
            if (failedCourses.stream().noneMatch(course -> course.getId() != null
                    && course.getId().intValue() == selectedCourseId)) {
                addError("Recovery Plan", "Select a course from the student's failed-course list.");
                return;
            }

            int actorId = loginBean.getCurrentUser() != null
                    ? loginBean.getCurrentUser().getId().intValue() : 0;
            RecoveryPlan newPlan = recoveryService.createPlan(selectedStudentId, selectedCourseId, actorId);
            selectedPlanId = newPlan.getId().intValue();
            loadPlansByStudentInternal(true);
            selectPlan(selectedPlanId);
            addInfo("Recovery Plan", "Recovery plan created. Plan #" + selectedPlanId + " is ready for milestones.");
        } catch (ValidationException e) {
            addError("Recovery Plan", e.getMessage());
        } catch (Exception e) {
            addError("Recovery Plan", "An unexpected error occurred");
        }
    }

    public void addRecommendation() {
        if (selectedPlan == null) {
            addError("Recommendation", "Select a recovery plan first.");
            return;
        }
        try {
            String actor = currentActorUsername();
            newRecommendation.setPlanId(selectedPlan.getId());
            recoveryService.addRecommendation(newRecommendation, actor);
            newRecommendation = new RecoveryRecommendation();
            refreshRecommendations();
            addInfo("Recommendation", "Recommendation added successfully.");
        } catch (ValidationException e) {
            addError("Recommendation", e.getMessage());
        } catch (Exception e) {
            addError("Recommendation", "An unexpected error occurred");
        }
    }

    public void deleteRecommendation(long recId) {
        if (selectedPlan == null) {
            addError("Recommendation", "Select a recovery plan first.");
            return;
        }
        try {
            recoveryService.deleteRecommendation(recId, currentActorUsername());
            refreshRecommendations();
            addInfo("Recommendation", "Recommendation deleted successfully.");
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
                        "Recovery Plan Created - " + plan.getCourse().getCourseName(),
                        "Dear " + plan.getStudent().getFullName() + ",\n\n"
                        + "A recovery plan has been created for " + plan.getCourse().getCourseName()
                        + " (attempt " + plan.getAttemptNumber() + ").\nStart date: " + plan.getStartDate());
                addInfo("Email", "Plan notification sent to student.");
            });
        } catch (ValidationException e) {
            addError("Email", e.getMessage());
        } catch (Exception e) {
            addError("Email", "Failed to send email: " + e.getMessage());
        }
    }

    public void prepareEditRecommendation(RecoveryRecommendation rec) {
        editingRecommendationId = rec.getId();
        editingRecommendationText = rec.getRecommendation();
    }

    public void saveEditedRecommendation() {
        if (editingRecommendationId == null) {
            return;
        }
        try {
            RecoveryRecommendation rec = new RecoveryRecommendation();
            rec.setId(editingRecommendationId);
            rec.setRecommendation(editingRecommendationText);
            recoveryService.updateRecommendation(rec, currentActorUsername());
            cancelEdit();
            refreshRecommendations();
            addInfo("Recommendation", "Recommendation updated successfully.");
        } catch (Exception e) {
            addError("Recommendation", "Failed to update recommendation.");
        }
    }

    public void cancelEdit() {
        editingRecommendationId = null;
        editingRecommendationText = "";
    }

    public boolean isEditingRecommendation(RecoveryRecommendation rec) {
        return editingRecommendationId != null && editingRecommendationId.equals(rec.getId());
    }

    public void addMilestone() {
        if (selectedPlanId <= 0) {
            addError("Milestone", "Please select a recovery plan first.");
            return;
        }
        if (newMilestone.getTitle() == null || newMilestone.getTitle().isBlank()) {
            addError("Milestone", "Please enter a milestone title.");
            return;
        }
        try {
            newMilestone.setRecoveryPlanId((long) selectedPlanId);
            recoveryService.addMilestone(newMilestone, currentActorUsername());
            newMilestone = new Milestone();
            refreshAfterPlanMutation();
            addInfo("Milestone", "Milestone added to Plan #" + selectedPlanId + ".");
        } catch (ValidationException e) {
            addError("Milestone", e.getMessage());
        } catch (Exception e) {
            addError("Milestone", "An unexpected error occurred");
        }
    }

    public boolean allMilestonesCompleted(int planId) {
        if (planId <= 0) {
            return false;
        }
        List<Milestone> planMilestones = planId == selectedPlanId
                ? milestones
                : recoveryService.findById(planId).map(RecoveryPlan::getMilestones).orElse(new ArrayList<>());

        if (planMilestones == null || planMilestones.isEmpty()) {
            return false;
        }

        for (Milestone milestone : planMilestones) {
            if (milestone.getStatus() == null) {
                return false;
            }
            String status = milestone.getStatus().name();
            if (!"DONE".equals(status) && !"COMPLETED".equals(status)) {
                return false;
            }
        }
        return true;
    }

    public boolean canUpdateGrade(RecoveryPlan plan) {
        if (plan == null || plan.getStatus() == null || !"ACTIVE".equals(plan.getStatus().name())) {
            return false;
        }
        List<Milestone> planMilestones = plan.getMilestones();
        if (planMilestones == null || planMilestones.isEmpty()) {
            return false;
        }
        for (Milestone milestone : planMilestones) {
            if (milestone.getStatus() == null) {
                return false;
            }
            String status = milestone.getStatus().name();
            if (!"DONE".equals(status) && !"COMPLETED".equals(status)) {
                return false;
            }
        }
        return true;
    }

    public void selectPlan(int planId) {
        if (planId <= 0) {
            clearPlanSelection();
            return;
        }
        selectedPlanId = planId;
        refreshSelectedPlanState();
        loadMilestones();
        refreshRecommendations();
    }

    public void onRowSelect(SelectEvent<RecoveryPlan> event) {
        RecoveryPlan plan = event.getObject();
        if (plan != null && plan.getId() != null) {
            selectPlan(plan.getId().intValue());
        }
    }

    public void updateMilestoneStatus(int milestoneId, MilestoneStatus status) {
        if (selectedPlanId <= 0 || selectedPlan == null) {
            addError("Milestone", "Select a recovery plan before updating milestones.");
            return;
        }
        try {
            Milestone milestone = milestones.stream()
                    .filter(item -> item.getId() != null && item.getId().intValue() == milestoneId)
                    .findFirst()
                    .orElseThrow(() -> new ValidationException("Milestone not found for the selected plan."));

            recoveryService.updateMilestoneStatus(milestoneId, status, currentActorUsername());
            refreshAfterPlanMutation();
            addInfo("Milestone", "Marked \"" + milestone.getTitle() + "\" as " + status.name() + ".");
        } catch (ValidationException e) {
            addError("Milestone", e.getMessage());
        } catch (Exception e) {
            addError("Milestone", "An unexpected error occurred");
        }
    }

    public void prepareGradeUpdate(RecoveryPlan plan) {
        gradingPlan = plan;
        gradingPlanId = plan.getId().intValue();
        newGrade = "";
        newGradePoint = 0.0;
    }

    private double gradeToPoint(String grade) {
        return switch (grade) {
            case "A" -> 4.0;
            case "A-" -> 3.7;
            case "B+" -> 3.3;
            case "B" -> 3.0;
            case "B-" -> 2.7;
            case "C+" -> 2.3;
            case "C" -> 2.0;
            case "C-" -> 1.7;
            default -> 0.0;
        };
    }

    public void submitGradeUpdate() {
        if (gradingPlanId <= 0) {
            addError("Grade Update", "No plan selected.");
            return;
        }
        if (newGrade == null || newGrade.isBlank()) {
            addError("Grade Update", "Please select a grade.");
            return;
        }
        try {
            RecoveryPlan plan = recoveryService.findById(gradingPlanId)
                    .orElseThrow(() -> new ValidationException("Recovery plan not found."));
            newGradePoint = gradeToPoint(newGrade);
            int actorId = loginBean.getCurrentUser() != null
                    ? loginBean.getCurrentUser().getId().intValue() : 0;

            EligibilityDTO eligibility = recoveryService.updateStudentGrade(
                    gradingPlanId, newGrade, newGradePoint, actorId);

            if (!"F".equalsIgnoreCase(newGrade)) {
                addInfo("Grade Updated",
                        "Grade updated. Student passed on Attempt " + plan.getAttemptNumber() + ".");
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
                                "Grade updated. Student still needs recovery. Consider creating Attempt "
                                + (plan.getAttemptNumber() + 1) + "."));
            }

            int planToReselect = gradingPlanId;
            gradingPlan = null;
            gradingPlanId = 0;
            refreshPlans(true);
            selectPlan(planToReselect);
        } catch (ValidationException e) {
            addError("Grade Update", e.getMessage());
        } catch (Exception e) {
            addError("Grade Update", "An unexpected error occurred");
        }
    }

    public void onPlanSelect() {
        if (selectedPlanId <= 0) {
            clearPlanSelection();
            return;
        }
        selectPlan(selectedPlanId);
    }

    public void onStudentChange() {
        loadFailedCourses(selectedStudentId);
        selectedCourseId = 0;
        clearPlanSelection();
        if (selectedStudentId > 0) {
            loadPlansByStudentInternal(false);
        } else {
            loadAllPlansInternal(false);
        }
    }

    public void loadFailedCourses(int studentId) {
        if (studentId <= 0) {
            failedCourses = new ArrayList<>();
            failedCoursesMessage = "Select a student to load failed courses.";
            return;
        }
        try {
            List<Integer> ids = resultDAO.findFailedCourseIds(studentId);
            List<Course> failed = new ArrayList<>();
            for (int id : ids) {
                courseDAO.findById((long) id).ifPresent(failed::add);
            }
            failedCourses = failed;
            failedCoursesMessage = failed.isEmpty()
                    ? "This student has no failed courses available for recovery."
                    : null;
        } catch (Exception e) {
            failedCourses = new ArrayList<>();
            failedCoursesMessage = "Failed courses could not be loaded right now.";
        }
    }

    public String prepareFromEligibility() {
        String param = FacesContext.getCurrentInstance()
                .getExternalContext()
                .getRequestParameterMap()
                .get("studentId");
        if (param != null && !param.isEmpty()) {
            try {
                selectedStudentId = Integer.parseInt(param);
            } catch (NumberFormatException ignored) {
                selectedStudentId = 0;
            }
            return "/pages/recovery.xhtml?faces-redirect=true&studentId=" + selectedStudentId;
        }
        return "/pages/recovery.xhtml?faces-redirect=true";
    }

    public List<RecoveryPlan> getRecoveryPlans() {
        return recoveryPlans;
    }

    public RecoveryPlan getSelectedPlan() {
        return selectedPlan;
    }

    public void setSelectedPlan(RecoveryPlan selectedPlan) {
        if (selectedPlan == null || selectedPlan.getId() == null) {
            clearPlanSelection();
            return;
        }
        selectPlan(selectedPlan.getId().intValue());
    }

    public int getSelectedStudentId() {
        return selectedStudentId;
    }

    public void setSelectedStudentId(int selectedStudentId) {
        this.selectedStudentId = selectedStudentId;
    }

    public int getSelectedCourseId() {
        return selectedCourseId;
    }

    public void setSelectedCourseId(int selectedCourseId) {
        this.selectedCourseId = selectedCourseId;
    }

    public int getSelectedPlanId() {
        return selectedPlanId;
    }

    public void setSelectedPlanId(int selectedPlanId) {
        this.selectedPlanId = selectedPlanId;
    }

    public List<Student> getStudents() {
        return students;
    }

    public List<Course> getFailedCourses() {
        return failedCourses;
    }

    public String getFailedCoursesMessage() {
        return failedCoursesMessage;
    }

    public RecoveryRecommendation getNewRecommendation() {
        return newRecommendation;
    }

    public List<RecoveryRecommendation> getRecommendations() {
        return recommendations;
    }

    public Long getEditingRecommendationId() {
        return editingRecommendationId;
    }

    public String getEditingRecommendationText() {
        return editingRecommendationText;
    }

    public void setEditingRecommendationText(String editingRecommendationText) {
        this.editingRecommendationText = editingRecommendationText;
    }

    public Milestone getNewMilestone() {
        return newMilestone;
    }

    public String getNewGrade() {
        return newGrade;
    }

    public void setNewGrade(String newGrade) {
        this.newGrade = newGrade;
    }

    public RecoveryPlan getGradingPlan() {
        if (gradingPlan != null) {
            return gradingPlan;
        }
        if (gradingPlanId > 0) {
            gradingPlan = recoveryService.findById(gradingPlanId).orElse(null);
        }
        return gradingPlan;
    }

    public List<Milestone> getMilestones() {
        return milestones;
    }

    public boolean isMilestoneActionable(Milestone milestone) {
        if (milestone == null || milestone.getStatus() == null) {
            return false;
        }
        return milestone.getStatus() == MilestoneStatus.PENDING
                || milestone.getStatus() == MilestoneStatus.OVERDUE;
    }

    private void loadMilestones() {
        if (selectedPlanId <= 0) {
            milestones = new ArrayList<>();
            return;
        }
        try {
            milestones = recoveryService.findById(selectedPlanId)
                    .map(RecoveryPlan::getMilestones)
                    .orElse(new ArrayList<>());
        } catch (Exception e) {
            milestones = new ArrayList<>();
        }
    }

    private void refreshRecommendations() {
        if (selectedPlan == null || selectedPlan.getId() == null) {
            recommendations = new ArrayList<>();
            return;
        }
        try {
            recommendations = recoveryService.getRecommendations(selectedPlan.getId().intValue());
        } catch (Exception e) {
            recommendations = new ArrayList<>();
        }
    }

    private void loadAllPlansInternal(boolean preserveSelection) {
        recoveryPlans = recoveryService.findAll();
        syncSelectionAfterPlanReload(preserveSelection);
    }

    private void loadPlansByStudentInternal(boolean preserveSelection) {
        recoveryPlans = recoveryService.findByStudentId(selectedStudentId);
        syncSelectionAfterPlanReload(preserveSelection);
    }

    private void syncSelectionAfterPlanReload(boolean preserveSelection) {
        if (!preserveSelection || selectedPlanId <= 0) {
            clearPlanSelection();
            return;
        }
        boolean exists = recoveryPlans.stream().anyMatch(plan -> plan.getId() != null
                && plan.getId().intValue() == selectedPlanId);
        if (!exists) {
            clearPlanSelection();
            return;
        }
        refreshSelectedPlanState();
        loadMilestones();
        refreshRecommendations();
    }

    private void refreshPlans(boolean preserveSelection) {
        if (selectedStudentId > 0) {
            loadPlansByStudentInternal(preserveSelection);
        } else {
            loadAllPlansInternal(preserveSelection);
        }
    }

    private void refreshAfterPlanMutation() {
        refreshPlans(true);
        if (selectedPlanId > 0) {
            selectPlan(selectedPlanId);
        }
    }

    private void refreshSelectedPlanState() {
        selectedPlan = recoveryPlans.stream()
                .filter(plan -> plan.getId() != null && plan.getId().intValue() == selectedPlanId)
                .findFirst()
                .orElseGet(() -> recoveryService.findById(selectedPlanId).orElse(null));
        if (selectedPlan == null) {
            clearPlanSelection();
        }
    }

    private void autoSelectFirstPlan() {
        if (recoveryPlans.isEmpty() || selectedPlanId > 0) {
            return;
        }
        RecoveryPlan firstActive = recoveryPlans.stream()
                .filter(plan -> plan.getStatus() != null && "ACTIVE".equals(plan.getStatus().name()))
                .findFirst()
                .orElse(recoveryPlans.get(0));
        selectPlan(firstActive.getId().intValue());
    }

    private void clearPlanSelection() {
        selectedPlanId = 0;
        selectedPlan = null;
        milestones = new ArrayList<>();
        recommendations = new ArrayList<>();
    }

    private String currentActorUsername() {
        return loginBean.getCurrentUser() != null ? loginBean.getCurrentUser().getUsername() : "system";
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
