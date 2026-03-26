package com.epda.crs.dao;

import com.epda.crs.config.DBConnection;
import jakarta.enterprise.context.Dependent;
import com.epda.crs.model.Course;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import jakarta.ejb.Stateless;

@Stateless
public class EnrollmentDAO {

    /**
     * Inserts a new ENROLLED row for the given student and course.
     * Called immediately after a student is determined to be eligible.
     */
    public void enroll(int studentId, int courseId) {
        String sql = "INSERT INTO enrollments (student_id, course_id, status) VALUES (?, ?, 'ENROLLED')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, courseId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("EnrollmentDAO.enroll failed", e);
        }
    }

    /**
     * Returns all courses a student is currently enrolled in.
     */
    public List<Course> findByStudentId(int studentId) {
        String sql = "SELECT c.course_id, c.course_code, c.course_name, c.credit_hours " +
                     "FROM   enrollments e " +
                     "JOIN   courses c ON e.course_id = c.course_id " +
                     "WHERE  e.student_id = ? " +
                     "ORDER BY c.course_code";
        List<Course> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Course c = new Course();
                    c.setId((long) rs.getInt("course_id"));
                    c.setCourseCode(rs.getString("course_code"));
                    c.setCourseName(rs.getString("course_name"));
                    c.setCreditHours(rs.getInt("credit_hours"));
                    c.setGrade("");
                    c.setGradePoint(0.0);
                    list.add(c);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("EnrollmentDAO.findByStudentId failed", e);
        }
        return list;
    }
}
