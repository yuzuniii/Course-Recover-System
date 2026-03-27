package com.epda.crs.dao;

import com.epda.crs.config.DBConnection;
import com.epda.crs.model.Course;
import com.epda.crs.model.FailedComponent;
import com.epda.crs.util.CGPACalculator;
import jakarta.enterprise.context.Dependent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import jakarta.ejb.Stateless;

@Stateless
public class ResultDAO {

    /**
     * Returns grade-point/credit-hour pairs for every result row belonging to
     * this student. Used by CGPACalculator.calculate().
     */
    public List<CGPACalculator.StudentResult> findByStudentId(int studentId) {
        String sql = "SELECT scr.grade_point, c.credit_hours " +
                     "FROM   student_course_results scr " +
                     "JOIN   courses c ON scr.course_id = c.course_id " +
                     "WHERE  scr.student_id = ?";
        List<CGPACalculator.StudentResult> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new CGPACalculator.StudentResult(
                        rs.getDouble("grade_point"),
                        rs.getInt("credit_hours")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("ResultDAO.findByStudentId failed", e);
        }
        return list;
    }

    /**
     * Counts rows where grade = 'F' for the given student.
     * Used by EligibilityService to enforce the failedCourseCount <= 3 rule.
     */
    public int countFailedCourses(int studentId) {
        String sql = "SELECT COUNT(*) FROM student_course_results " +
                     "WHERE student_id = ? AND grade = 'F'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("ResultDAO.countFailedCourses failed", e);
        }
        return 0;
    }

    /**
     * Returns course results for a specific student and semester,
     * with grade/gradePoint populated from student_course_results.
     * Used by ReportService to build AcademicReportDTO rows.
     */
    /**
     * Returns the course_id of every course the student has failed (grade = 'F').
     * Used by EligibilityService to enroll the student in recovery courses.
     */
    public List<Integer> findFailedCourseIds(int studentId) {
        String sql = "SELECT DISTINCT course_id FROM student_course_results " +
                     "WHERE student_id = ? AND grade = 'F'";
        List<Integer> ids = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("course_id"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("ResultDAO.findFailedCourseIds failed", e);
        }
        return ids;
    }

    /**
     * Full result projection used by ReportService to build AcademicReportDTO rows.
     * Includes attempt_number and status from student_course_results.
     */
    public record FullResult(String courseCode, String courseName, int creditHours,
                             String grade, double gradePoint,
                             int attemptNumber, String resultStatus) {}

    /**
     * Returns full result rows for a student filtered by semester number (1 or 2).
     * Matches rows where the semester column ends with "/" + semesterNum, e.g. "2025/1".
     * Used by ReportService to scope reports to a specific semester.
     */
    public List<FullResult> getFullResultsByStudentAndSemester(int studentId, int semesterNum) {
        String sql = "SELECT c.course_code, c.course_name, c.credit_hours, " +
                     "       scr.grade, scr.grade_point, scr.attempt_number, scr.status " +
                     "FROM   student_course_results scr " +
                     "JOIN   courses c ON scr.course_id = c.course_id " +
                     "WHERE  scr.student_id = ? AND scr.semester LIKE ? " +
                     "ORDER BY c.course_code";
        List<FullResult> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, "%/" + semesterNum);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new FullResult(
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        rs.getInt("credit_hours"),
                        rs.getString("grade"),
                        rs.getDouble("grade_point"),
                        rs.getInt("attempt_number"),
                        rs.getString("status")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("ResultDAO.getFullResultsByStudentAndSemester failed", e);
        }
        return list;
    }

    public List<FullResult> getFullResultsByStudentId(int studentId) {
        String sql = "SELECT c.course_code, c.course_name, c.credit_hours, " +
                     "       scr.grade, scr.grade_point, scr.attempt_number, scr.status " +
                     "FROM   student_course_results scr " +
                     "JOIN   courses c ON scr.course_id = c.course_id " +
                     "WHERE  scr.student_id = ? " +
                     "ORDER BY c.course_code";
        List<FullResult> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new FullResult(
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        rs.getInt("credit_hours"),
                        rs.getString("grade"),
                        rs.getDouble("grade_point"),
                        rs.getInt("attempt_number"),
                        rs.getString("status")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("ResultDAO.getFullResultsByStudentId failed", e);
        }
        return list;
    }

    /**
     * Returns distinct semester numbers (int) from eligibility_records for this student.
     * Used by EligibilityBean to populate the semester dropdown dynamically.
     */
    public List<Integer> getDistinctSemestersByStudent(int studentId) {
        String sql = "SELECT DISTINCT semester FROM eligibility_records " +
                     "WHERE student_id = ? ORDER BY semester";
        List<Integer> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getInt("semester"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("ResultDAO.getDistinctSemestersByStudent failed", e);
        }
        return list;
    }

    /**
     * Returns distinct year_of_study values (int) from eligibility_records for this student.
     * Used by EligibilityBean to populate the year dropdown dynamically.
     */
    public List<Integer> getDistinctYearsByStudent(int studentId) {
        String sql = "SELECT DISTINCT year_of_study FROM eligibility_records " +
                     "WHERE student_id = ? ORDER BY year_of_study";
        List<Integer> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getInt("year_of_study"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("ResultDAO.getDistinctYearsByStudent failed", e);
        }
        return list;
    }

    /**
     * Returns all failed_components rows for every failed course of the given student.
     * Joins failed_components → student_course_results to filter by student_id.
     */
    public List<FailedComponent> getFailedComponentsByStudentId(int studentId) {
        String sql = "SELECT fc.component_id, fc.result_id, fc.component_name, " +
                     "       fc.component_score, fc.pass_required " +
                     "FROM   failed_components fc " +
                     "JOIN   student_course_results scr ON fc.result_id = scr.result_id " +
                     "WHERE  scr.student_id = ? AND scr.grade = 'F' " +
                     "ORDER BY scr.course_id, fc.component_id";
        List<FailedComponent> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FailedComponent fc = new FailedComponent();
                    fc.setComponentId(rs.getInt("component_id"));
                    fc.setResultId(rs.getInt("result_id"));
                    fc.setComponentName(rs.getString("component_name"));
                    fc.setComponentScore(rs.getDouble("component_score"));
                    fc.setPassRequired(rs.getDouble("pass_required"));
                    list.add(fc);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("ResultDAO.getFailedComponentsByStudentId failed", e);
        }
        return list;
    }

    /**
     * Updates grade, grade_point, and status for a specific result row.
     * Derives status from the grade: 'F' → FAILED, anything else → PASSED.
     */
    public void updateGrade(int studentId, int courseId, int attemptNumber,
                            String newGrade, double newGradePoint) {
        String sql = "UPDATE student_course_results " +
                     "SET grade = ?, grade_point = ?, status = ? " +
                     "WHERE student_id = ? AND course_id = ? AND attempt_number = ?";
        String status = "F".equalsIgnoreCase(newGrade) ? "FAILED" : "PASSED";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newGrade);
            ps.setDouble(2, newGradePoint);
            ps.setString(3, status);
            ps.setInt(4, studentId);
            ps.setInt(5, courseId);
            ps.setInt(6, attemptNumber);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("ResultDAO.updateGrade failed", e);
        }
    }

    public List<Course> getResultsByStudentAndSemester(int studentId, String semester) {
        String sql = "SELECT c.course_id, c.course_code, c.course_name, c.credit_hours, " +
                     "       scr.grade, scr.grade_point " +
                     "FROM   student_course_results scr " +
                     "JOIN   courses c ON scr.course_id = c.course_id " +
                     "WHERE  scr.student_id = ? AND scr.semester = ? " +
                     "ORDER BY c.course_code";
        List<Course> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, semester);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Course c = new Course();
                    c.setId((long) rs.getInt("course_id"));
                    c.setCourseCode(rs.getString("course_code"));
                    c.setCourseName(rs.getString("course_name"));
                    c.setCreditHours(rs.getInt("credit_hours"));
                    c.setGrade(rs.getString("grade"));
                    c.setGradePoint(rs.getDouble("grade_point"));
                    list.add(c);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("ResultDAO.getResultsByStudentAndSemester failed", e);
        }
        return list;
    }
}
