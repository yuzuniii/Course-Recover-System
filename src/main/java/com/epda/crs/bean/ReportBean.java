package com.epda.crs.bean;

import com.epda.crs.dto.AcademicReportDTO;
import com.epda.crs.exception.ValidationException;
import com.epda.crs.model.Student;
import com.epda.crs.service.ReportService;
import com.epda.crs.util.EmailUtil;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;

@Named
@ViewScoped
public class ReportBean implements Serializable {

    @EJB
    private ReportService reportService;

    private int selectedStudentId;
    private int selectedSemester;
    private int selectedYear;
    private AcademicReportDTO currentReport;
    private List<AcademicReportDTO> allReports;
    private List<Student> students;

    @PostConstruct
    public void init() {
        try {
            students = reportService.getStudents();
        } catch (Exception e) {
            addError("Initialisation", "An unexpected error occurred");
        }
    }

    public void generateReport() {
        try {
            currentReport = reportService.generateReport(
                    selectedStudentId, selectedSemester, selectedYear);
            FacesContext.getCurrentInstance().getExternalContext()
                    .getSessionMap().put("currentReport", currentReport);
            addInfo("Report", "Report generated successfully");
        } catch (ValidationException e) {
            addError("Report", e.getMessage());
        } catch (Exception e) {
            addError("Report", "An unexpected error occurred");
        }
    }

    public void generateAllReports() {
        try {
            allReports = reportService.generateAllReports(selectedSemester, selectedYear);
            addInfo("Reports", allReports.size() + " report(s) generated successfully");
        } catch (ValidationException e) {
            addError("Reports", e.getMessage());
        } catch (Exception e) {
            addError("Reports", "An unexpected error occurred");
        }
    }

    public void sendReportEmail() {
        System.err.println("[ReportBean] sendReportEmail called");
        try {
            if (currentReport == null) {
                addError("Email", "No report generated yet. Generate a report first.");
                return;
            }
            String recipient = currentReport.getStudentEmail();
            double cgpa = currentReport.getCgpa();
            String standing = cgpa >= 3.5 ? "Excellent Standing"
                    : cgpa >= 3.0 ? "Good Standing"
                    : cgpa >= 2.0 ? "Satisfactory Standing"
                    : "At Risk";
            String subject = "Academic Report \u2014 Semester "
                    + currentReport.getSemester()
                    + " Year " + currentReport.getYearOfStudy();
            String html = EmailUtil.buildReportEmailHtml(
                    currentReport.getStudentName(),
                    currentReport.getStudentCode(),
                    currentReport.getProgramme(),
                    currentReport.getSemester(),
                    currentReport.getYearOfStudy(),
                    cgpa,
                    standing,
                    currentReport.getResults());
            System.err.println("[ReportBean] sendReportEmail: sending to " + recipient);
            EmailUtil.sendEmailHtml(recipient, subject, html);
            addInfo("Email", "Report notification sent successfully.");
        } catch (Exception e) {
            addError("Email", "Failed to send notification: " + e.getMessage());
        }
    }

    public void clearReport() {
        currentReport = null;
        addInfo("Report", "Report cleared");
    }

    public int getSelectedStudentId() { return selectedStudentId; }
    public void setSelectedStudentId(int selectedStudentId) { this.selectedStudentId = selectedStudentId; }

    public int getSelectedSemester() { return selectedSemester; }
    public void setSelectedSemester(int selectedSemester) { this.selectedSemester = selectedSemester; }

    public int getSelectedYear() { return selectedYear; }
    public void setSelectedYear(int selectedYear) { this.selectedYear = selectedYear; }

    public AcademicReportDTO getCurrentReport() { return currentReport; }
    public void setCurrentReport(AcademicReportDTO currentReport) { this.currentReport = currentReport; }

    public List<AcademicReportDTO> getAllReports() { return allReports; }
    public void setAllReports(List<AcademicReportDTO> allReports) { this.allReports = allReports; }

    public List<Student> getStudents() { return students; }
    public void setStudents(List<Student> students) { this.students = students; }

    private void addInfo(String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, summary, detail));
    }

    private void addError(String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, detail));
    }
}
