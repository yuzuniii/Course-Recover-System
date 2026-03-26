package com.epda.crs.service;

import com.epda.crs.dao.EligibilityDAO;
import com.epda.crs.dao.EnrollmentDAO;
import com.epda.crs.dao.ResultDAO;
import com.epda.crs.dao.StudentDAO;
import com.epda.crs.dto.EligibilityDTO;
import com.epda.crs.exception.ValidationException;
import com.epda.crs.model.Student;
import com.epda.crs.util.CGPACalculator;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
public class EligibilityService {

    @Inject private StudentDAO     studentDAO;
    @Inject private ResultDAO      resultDAO;
    @Inject private EligibilityDAO eligibilityDAO;
    @Inject private EnrollmentDAO  enrollmentDAO;

    @Inject
    private AuditLogService auditLogService;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Orchestrates the full eligibility check for one student:
     *   1. calculateEligibility  — computes CGPA, failed count, and rule outcome
     *   2. persistEligibility    — writes the result to eligibility_records
     *   3. enrolIfEligible       — creates enrollment rows if the student qualifies
     *   4. AuditLogService       — records the action
     *
     * @throws ValidationException if studentId is invalid or the student is not found
     */
    public EligibilityDTO checkEligibility(int studentId, int semester, int yearOfStudy, String actorUsername) {
        if (studentId <= 0) throw new ValidationException("Invalid student ID");

        // Always compute — this is what the UI needs to display
        EligibilityDTO dto = calculateEligibility(studentId, semester, yearOfStudy);

        // Persistence is a side-effect; do not let DB errors prevent the result being returned
        try {
            persistEligibility(dto);
        } catch (Exception e) {
            // non-fatal: result is still returned to the caller
        }

        try {
            enrolIfEligible(dto);
        } catch (Exception e) {
            // non-fatal: enrollment failure does not block the eligibility result
        }

        try {
            if (auditLogService != null) {
                auditLogService.logAction(
                        actorUsername,
                        "CHECK_ELIGIBILITY",
                        "ELIGIBILITY_RECORD",
                        (long) studentId,
                        "Eligibility checked for student " + studentId + ": " + dto.getReason());
            }
        } catch (Exception e) {
            // non-fatal
        }

        return dto;
    }

    /**
     * Returns all students whose live computed CGPA < 2.0 or failedCourseCount > 3.
     */
    public List<Student> getIneligibleStudents() {
        return studentDAO.findAll().stream()
                .map(s -> {
                    // Compute live values and stamp them onto the Student so the
                    // UI displays accurate figures, not whatever is stored in the DB.
                    List<CGPACalculator.StudentResult> res =
                            resultDAO.findByStudentId(s.getId().intValue());
                    double cgpa   = CGPACalculator.calculate(res);
                    int    failed = resultDAO.countFailedCourses(s.getId().intValue());
                    s.setCgpa(cgpa);
                    s.setFailedCourseCount(failed);
                    return s;
                })
                .filter(s -> !(s.getCgpa() >= 2.0 && s.getFailedCourseCount() <= 3))
                .collect(Collectors.toList());
    }

    // -----------------------------------------------------------------------
    // Private steps extracted from checkEligibility()
    // -----------------------------------------------------------------------

    /**
     * Step 1 — pure computation, no side effects.
     * Fetches the student, runs ResultDAO queries, applies eligibility rules,
     * and returns a populated (but not yet persisted) EligibilityDTO.
     */
    private EligibilityDTO calculateEligibility(int studentId, int semester, int yearOfStudy) {
        Student student = studentDAO.findById((long) studentId)
                .orElseThrow(() -> new ValidationException("Student not found: " + studentId));

        // Populate identity fields first so we can return early if needed
        EligibilityDTO dto = new EligibilityDTO();
        dto.setStudentId(studentId);
        dto.setStudentCode(student.getStudentNumber());
        dto.setStudentName(student.getFullName());
        dto.setSemester(semester);
        dto.setYearOfStudy(yearOfStudy);

        // Guard: no results recorded — cannot compute CGPA
        List<CGPACalculator.StudentResult> rawResults = resultDAO.findByStudentId(studentId);
        if (rawResults.isEmpty()) {
            dto.setCgpa(0.0);
            dto.setFailedCourses(0);
            dto.setEligible(false);
            dto.setReason("No academic results found for this semester");
            return dto;
        }

        double cgpa        = CGPACalculator.calculate(rawResults);
        int    failedCount = resultDAO.countFailedCourses(studentId);

        boolean eligible = cgpa >= 2.0 && failedCount <= 3;
        String reason;
        if (eligible) {
            reason = "Meets all progression requirements";
        } else {
            StringBuilder sb = new StringBuilder();
            if (cgpa < 2.0)        sb.append("CGPA below minimum requirement of 2.0. ");
            if (failedCount > 3)   sb.append("Exceeded maximum of 3 failed courses. ");
            reason = sb.toString().trim();
        }

        dto.setCgpa(cgpa);
        dto.setFailedCourseCount(failedCount);
        dto.setEligible(eligible);
        dto.setReason(reason);
        return dto;
    }

    /**
     * Step 2 — persistence only.
     * Saves the eligibility result to the eligibility_records table.
     */
    private void persistEligibility(EligibilityDTO dto) {
        eligibilityDAO.save(dto);
    }

    /**
     * Step 3 — conditional enrollment.
     * Calls EnrollmentDAO.enroll() for each failed course only when the
     * student is eligible; does nothing otherwise.
     */
    private void enrolIfEligible(EligibilityDTO dto) {
        if (!dto.isEligible()) return;
        for (int courseId : resultDAO.findFailedCourseIds(dto.getStudentId())) {
            enrollmentDAO.enroll(dto.getStudentId(), courseId);
        }
    }

    // -----------------------------------------------------------------------
    // Legacy helpers (used by DashboardService)
    // -----------------------------------------------------------------------

    /**
     * Quick check using the Student's stored cgpa and failedCourseCount fields.
     * Used by DashboardService analytics. For accurate figures use checkEligibility().
     */
    public boolean isEligible(Student student) {
        return student.getCgpa() >= 2.0 && student.getFailedCourseCount() <= 3;
    }

    /** Returns an eligibility breakdown for all students (uses stored field values). */
    public List<EligibilityDTO> getEligibilityBreakdown() {
        return studentDAO.findAll().stream().map(s -> {
            EligibilityDTO dto = new EligibilityDTO();
            dto.setStudentId(s.getId().intValue());
            dto.setStudentCode(s.getStudentNumber());
            dto.setStudentName(s.getFullName());
            dto.setCgpa(s.getCgpa());
            dto.setFailedCourseCount(s.getFailedCourseCount());
            dto.setEligible(isEligible(s));
            dto.setReason(dto.isEligible()
                    ? "Eligible for progression."
                    : "Requires recovery due to CGPA or failed course count.");
            return dto;
        }).collect(Collectors.toList());
    }
}
