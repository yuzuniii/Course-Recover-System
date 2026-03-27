package com.epda.crs.bean;

import com.epda.crs.dao.ResultDAO;
import com.epda.crs.dto.EligibilityDTO;
import com.epda.crs.util.EmailUtil;
import com.epda.crs.exception.ValidationException;
import com.epda.crs.model.FailedComponent;
import com.epda.crs.model.Student;
import com.epda.crs.service.EligibilityService;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

@Named
@ViewScoped
public class EligibilityBean implements Serializable {

    @EJB
    private EligibilityService eligibilityService;

    @EJB
    private ResultDAO resultDAO;

    @Inject
    private LoginBean loginBean;

    private int selectedStudentId;
    private int selectedSemester;
    private int selectedYear;
    private EligibilityDTO eligibilityResult;
    private java.util.List<Student> ineligibleStudents;
    private java.util.List<EligibilityDTO> allStudentDtos;
    private String enrollmentMessage;
    private List<Integer> availableSemesters;
    private List<Integer> availableYears;
    private List<FailedComponent> failedComponents;

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @PostConstruct
    public void init() {
        availableSemesters = Arrays.asList(1, 2);
        availableYears = Arrays.asList(1, 2, 3);
        try {
            allStudentDtos = eligibilityService.getEligibilityBreakdown();
        } catch (Exception e) {
            // non-fatal: dropdown will be empty
        }
        loadIneligibleStudents();
    }

    // -----------------------------------------------------------------------
    // Actions
    // -----------------------------------------------------------------------

    public void onStudentChange() {
        if (selectedStudentId <= 0) {
            availableSemesters = Arrays.asList(1, 2);
            availableYears = Arrays.asList(1, 2, 3);
            return;
        }
        try {
            List<Integer> sems = resultDAO.getDistinctSemestersByStudent(selectedStudentId);
            availableSemesters = sems.isEmpty() ? Arrays.asList(1, 2) : sems;
        } catch (Exception e) {
            availableSemesters = Arrays.asList(1, 2);
        }
        try {
            List<Integer> years = resultDAO.getDistinctYearsByStudent(selectedStudentId);
            availableYears = years.isEmpty() ? Arrays.asList(1, 2, 3) : years;
        } catch (Exception e) {
            availableYears = Arrays.asList(1, 2, 3);
        }
    }

    public void checkEligibility() {
        try {
            String actor = (loginBean != null && loginBean.getCurrentUser() != null) 
                    ? loginBean.getCurrentUser().getUsername() : "system";
            
            eligibilityResult = eligibilityService.checkEligibility(
                    selectedStudentId, selectedSemester, selectedYear, actor);

            try {
                failedComponents = resultDAO.getFailedComponentsByStudentId(selectedStudentId);
            } catch (Exception ex) {
                failedComponents = java.util.Collections.emptyList();
            }

            enrollmentMessage = eligibilityResult.isEligible()
                    ? "Student is eligible and has been enrolled in failed courses."
                    : "Student is not eligible: " + eligibilityResult.getReason();

            FacesMessage.Severity severity = eligibilityResult.isEligible()
                    ? FacesMessage.SEVERITY_INFO
                    : FacesMessage.SEVERITY_WARN;
            addMessage(severity, "Eligibility Check", enrollmentMessage);

        } catch (ValidationException e) {
            addError("Eligibility Check", e.getMessage());
        } catch (Exception e) {
            addError("Eligibility Check", "An unexpected error occurred");
        }
    }

    public void sendEligibilityEmail() {
        if (eligibilityResult == null) {
            addError("Email", "No eligibility result to send. Run the check first.");
            return;
        }
        try {
            EmailUtil.sendEmail(
                    eligibilityResult.getStudentCode() + "@student.crs.local",
                    "Eligibility Check Result — " + eligibilityResult.getStudentName(),
                    "Dear " + eligibilityResult.getStudentName() + ",\n\n" +
                    "Your eligibility check result for Semester " + eligibilityResult.getSemester() +
                    ", Year " + eligibilityResult.getYearOfStudy() + " has been determined.\n\n" +
                    "Result: " + (eligibilityResult.isEligible() ? "ELIGIBLE" : "NOT ELIGIBLE") + "\n" +
                    "CGPA: " + String.format("%.2f", eligibilityResult.getCgpa()) + "\n" +
                    "Reason: " + eligibilityResult.getReason());
            addMessage(FacesMessage.SEVERITY_INFO, "Email", "Eligibility result sent to student.");
        } catch (Exception e) {
            addError("Email", "Failed to send email: " + e.getMessage());
        }
    }

    public void loadIneligibleStudents() {
        try {
            ineligibleStudents = eligibilityService.getIneligibleStudents();
            addMessage(FacesMessage.SEVERITY_INFO, "Ineligible Students",
                    ineligibleStudents.size() + " ineligible student(s) loaded.");
        } catch (ValidationException e) {
            addError("Ineligible Students", e.getMessage());
        } catch (Exception e) {
            addError("Ineligible Students", "An unexpected error occurred");
        }
    }

    // -----------------------------------------------------------------------
    // Getters and setters
    // -----------------------------------------------------------------------

    public int getSelectedStudentId() { return selectedStudentId; }
    public void setSelectedStudentId(int selectedStudentId) { this.selectedStudentId = selectedStudentId; }

    public int getSelectedSemester() { return selectedSemester; }
    public void setSelectedSemester(int selectedSemester) { this.selectedSemester = selectedSemester; }

    public int getSelectedYear() { return selectedYear; }
    public void setSelectedYear(int selectedYear) { this.selectedYear = selectedYear; }

    public EligibilityDTO getEligibilityResult() { return eligibilityResult; }
    public void setEligibilityResult(EligibilityDTO eligibilityResult) { this.eligibilityResult = eligibilityResult; }

    public List<Student> getIneligibleStudents() { return ineligibleStudents; }
    public void setIneligibleStudents(List<Student> ineligibleStudents) { this.ineligibleStudents = ineligibleStudents; }

    public List<EligibilityDTO> getAllStudentDtos() { return allStudentDtos; }
    public void setAllStudentDtos(List<EligibilityDTO> allStudentDtos) { this.allStudentDtos = allStudentDtos; }

    public String getEnrollmentMessage() { return enrollmentMessage; }
    public void setEnrollmentMessage(String enrollmentMessage) { this.enrollmentMessage = enrollmentMessage; }

    public List<Integer> getAvailableSemesters() { return availableSemesters; }
    public void setAvailableSemesters(List<Integer> availableSemesters) { this.availableSemesters = availableSemesters; }

    public List<Integer> getAvailableYears() { return availableYears; }
    public void setAvailableYears(List<Integer> availableYears) { this.availableYears = availableYears; }

    public List<FailedComponent> getFailedComponents() { return failedComponents; }
    public void setFailedComponents(List<FailedComponent> failedComponents) { this.failedComponents = failedComponents; }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(severity, summary, detail));
    }

    private void addError(String summary, String detail) {
        addMessage(FacesMessage.SEVERITY_ERROR, summary, detail);
    }
}
