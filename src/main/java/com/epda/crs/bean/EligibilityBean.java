package com.epda.crs.bean;

import com.epda.crs.dto.EligibilityDTO;
import com.epda.crs.exception.ValidationException;
import com.epda.crs.model.Student;
import com.epda.crs.service.EligibilityService;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;

import jakarta.inject.Inject;

@Named
@ViewScoped
public class EligibilityBean implements Serializable {

    @Inject
    private EligibilityService eligibilityService;

    @Inject
    private LoginBean loginBean;

    private int selectedStudentId;
    private int selectedSemester;
    private int selectedYear;
    private EligibilityDTO eligibilityResult;
    private java.util.List<Student> ineligibleStudents;
    private java.util.List<EligibilityDTO> allStudentDtos;
    private String enrollmentMessage;

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @PostConstruct
    public void init() {
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

    public void checkEligibility() {
        try {
            String actor = (loginBean != null && loginBean.getCurrentUser() != null) 
                    ? loginBean.getCurrentUser().getUsername() : "system";
            
            eligibilityResult = eligibilityService.checkEligibility(
                    selectedStudentId, selectedSemester, selectedYear, actor);

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
