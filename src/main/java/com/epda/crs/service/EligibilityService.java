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

    @Inject private StudentDAO studentDAO;
    @Inject private ResultDAO resultDAO;
    @Inject private EligibilityDAO eligibilityDAO;
    @Inject private EnrollmentDAO enrollmentDAO;

    @Inject
    private AuditLogService auditLogService;

    public EligibilityDTO checkEligibility(int studentId, int semester, int yearOfStudy, String actorUsername) {
        if (studentId <= 0) {
            throw new ValidationException("Invalid student ID");
        }

        EligibilityDTO dto = calculateEligibility(studentId, semester, yearOfStudy);

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

    public List<Student> getIneligibleStudents() {
        return studentDAO.findAll().stream()
                .map(s -> {
                    List<CGPACalculator.StudentResult> results =
                            resultDAO.findByStudentId(s.getId().intValue());

                    double cgpa;
                    int failedCount;
                    if (results.isEmpty()) {
                        cgpa = s.getCgpa();
                        failedCount = s.getFailedCourseCount();
                    } else {
                        cgpa = CGPACalculator.calculate(results);
                        failedCount = resultDAO.countFailedCourses(s.getId().intValue());
                    }

                    s.setCgpa(cgpa);
                    s.setFailedCourseCount(failedCount);
                    return s;
                })
                .filter(s -> !(s.getCgpa() >= 2.0 && s.getFailedCourseCount() <= 3))
                .collect(Collectors.toList());
    }

    private EligibilityDTO calculateEligibility(int studentId, int semester, int yearOfStudy) {
        Student student = studentDAO.findById((long) studentId)
                .orElseThrow(() -> new ValidationException("Student not found: " + studentId));

        EligibilityDTO dto = new EligibilityDTO();
        dto.setStudentId(studentId);
        dto.setStudentCode(student.getStudentNumber());
        dto.setStudentName(student.getFullName());
        dto.setSemester(semester);
        dto.setYearOfStudy(yearOfStudy);

        List<CGPACalculator.StudentResult> rawResults = resultDAO.findByStudentId(studentId);
        if (rawResults.isEmpty()) {
            double cgpa = student.getCgpa();
            int failedCount = student.getFailedCourseCount();
            boolean eligible = cgpa >= 2.0 && failedCount <= 3;

            dto.setCgpa(cgpa);
            dto.setFailedCourseCount(failedCount);
            dto.setEligible(eligible);
            dto.setReason(eligible
                    ? "Using stored academic snapshot. Meets all progression requirements"
                    : buildReason(cgpa, failedCount, true));
            return dto;
        }

        double cgpa = CGPACalculator.calculate(rawResults);
        int failedCount = resultDAO.countFailedCourses(studentId);
        boolean eligible = cgpa >= 2.0 && failedCount <= 3;

        dto.setCgpa(cgpa);
        dto.setFailedCourseCount(failedCount);
        dto.setEligible(eligible);
        dto.setReason(eligible
                ? "Meets all progression requirements"
                : buildReason(cgpa, failedCount, false));
        return dto;
    }

    private String buildReason(double cgpa, int failedCount, boolean usingStoredSnapshot) {
        StringBuilder sb = new StringBuilder();
        if (usingStoredSnapshot) {
            sb.append("Using stored academic snapshot. ");
        }
        if (cgpa < 2.0) {
            sb.append("CGPA below minimum requirement of 2.0. ");
        }
        if (failedCount > 3) {
            sb.append("Exceeded maximum of 3 failed courses. ");
        }
        return sb.toString().trim();
    }

    private void persistEligibility(EligibilityDTO dto) {
        eligibilityDAO.save(dto);
    }

    private void enrolIfEligible(EligibilityDTO dto) {
        if (!dto.isEligible()) {
            return;
        }
        for (int courseId : resultDAO.findFailedCourseIds(dto.getStudentId())) {
            enrollmentDAO.enroll(dto.getStudentId(), courseId);
        }
    }

    public boolean isEligible(Student student) {
        return student.getCgpa() >= 2.0 && student.getFailedCourseCount() <= 3;
    }

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
