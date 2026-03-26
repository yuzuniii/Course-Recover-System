package com.epda.crs.service;

import com.epda.crs.dao.ResultDAO;
import com.epda.crs.dao.StudentDAO;
import com.epda.crs.dto.AcademicReportDTO;
import com.epda.crs.exception.ValidationException;
import com.epda.crs.model.Student;
import com.epda.crs.util.CGPACalculator;
import com.epda.crs.util.EmailUtil;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
public class ReportService {

    @Inject
    private StudentDAO studentDAO;

    @Inject
    private ResultDAO resultDAO;

    @EJB
    private AuditLogService auditLogService;

    // -----------------------------------------------------------------------
    // Report generation
    // -----------------------------------------------------------------------

    /**
     * Generates an academic report for a single student scoped to the given
     * semester and year. Results are filtered to that semester only.
     * Sends a notification email and writes an audit log entry.
     *
     * @throws ValidationException if studentId is invalid or student not found
     */
    public AcademicReportDTO generateReport(int studentId, int semester, int yearOfStudy) {
        if (studentId <= 0) throw new ValidationException("Invalid student ID");
        if (semester  <= 0) throw new ValidationException("Invalid semester");
        if (yearOfStudy <= 0) throw new ValidationException("Invalid year of study");

        Student student = studentDAO.findById((long) studentId)
                .orElseThrow(() -> new ValidationException("Student not found"));

        AcademicReportDTO report = buildReport(student, semester, yearOfStudy);

        // Email notification
        EmailUtil.sendEmail(
                student.getStudentNumber() + "@student.crs.local",
                "Academic Report — Semester " + semester + " Year " + yearOfStudy,
                "Dear " + student.getFullName() + ",\n\n" +
                "Your academic report for semester " + semester + " of year " + yearOfStudy +
                " has been generated. CGPA: " + String.format("%.2f", report.getCgpa()));

        // Audit log
        if (auditLogService != null) {
            auditLogService.logAction(
                    student.getStudentNumber(),
                    "GENERATE_REPORT",
                    "ACADEMIC_REPORT",
                    (long) studentId,
                    "Academic report generated for student " + studentId +
                    " (sem " + semester + ", year " + yearOfStudy + ")");
        }

        return report;
    }

    /**
     * Generates reports for all students filtered to the given semester and year.
     * Results for each student are scoped to that semester only.
     * Does not send individual emails (bulk operation).
     *
     * @throws ValidationException if semester or yearOfStudy are invalid
     */
    public List<AcademicReportDTO> generateAllReports(int semester, int yearOfStudy) {
        if (semester    <= 0) throw new ValidationException("Invalid semester");
        if (yearOfStudy <= 0) throw new ValidationException("Invalid year of study");

        return studentDAO.findAll().stream()
                .map(s -> buildReport(s, semester, yearOfStudy))
                .collect(Collectors.toList());
    }

    // -----------------------------------------------------------------------
    // Internal builder — no side effects
    // -----------------------------------------------------------------------

    /**
     * Builds an AcademicReportDTO for a student, filtering result rows to the
     * specified semester number via ResultDAO.getFullResultsByStudentAndSemester().
     * CGPA is recomputed from those filtered results.
     */
    private AcademicReportDTO buildReport(Student student, int semester, int yearOfStudy) {
        int studentId = student.getId().intValue();

        // Fetch only results matching the requested semester (e.g. LIKE "%/1")
        List<ResultDAO.FullResult> fullResults =
                resultDAO.getFullResultsByStudentAndSemester(studentId, semester);

        // Recompute CGPA from the filtered result set
        List<CGPACalculator.StudentResult> forCgpa = fullResults.stream()
                .map(r -> new CGPACalculator.StudentResult(r.gradePoint(), r.creditHours()))
                .collect(Collectors.toList());
        double cgpa = CGPACalculator.calculate(forCgpa);

        // Convert to report rows
        List<AcademicReportDTO.CourseResultRow> rows = fullResults.stream()
                .map(r -> new AcademicReportDTO.CourseResultRow(
                        r.courseCode(), r.courseName(), r.creditHours(),
                        r.grade(), r.gradePoint(),
                        r.attemptNumber(),
                        r.resultStatus() != null ? r.resultStatus() : ""))
                .collect(Collectors.toList());

        AcademicReportDTO report = new AcademicReportDTO();
        report.setStudentId(studentId);
        report.setStudentCode(student.getStudentNumber());
        report.setStudentName(student.getFullName());
        report.setProgramme(student.getProgramName());
        report.setSemester(semester);
        report.setYearOfStudy(yearOfStudy);
        report.setCgpa(cgpa);
        report.setResults(rows);
        return report;
    }

    // -----------------------------------------------------------------------
    // Legacy methods (used by ReportBean)
    // -----------------------------------------------------------------------

    /**
     * Generates a report using the student's stored current semester and year.
     * Kept for backward compatibility with ReportBean.
     */
    public AcademicReportDTO generateAcademicReport(Long studentId) {
        if (studentId == null || studentId <= 0) return null;
        Student student = studentDAO.findById(studentId).orElse(null);
        if (student == null) return null;
        return buildReport(student, student.getSemester(), student.getYearOfStudy());
    }

    /** Returns all students. Used by ReportBean to populate the student selector. */
    public List<Student> getStudents() {
        return studentDAO.findAll();
    }
}
