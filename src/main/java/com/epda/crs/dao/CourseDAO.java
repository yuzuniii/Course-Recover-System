package com.epda.crs.dao;

import com.epda.crs.config.DBConnection;
import com.epda.crs.model.Course;
import jakarta.enterprise.context.Dependent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Dependent
public class CourseDAO {

    private Course mapRow(ResultSet rs) throws SQLException {
        Course c = new Course();
        c.setId((long) rs.getInt("course_id"));
        c.setCourseCode(rs.getString("course_code"));
        c.setCourseName(rs.getString("course_name"));
        c.setCreditHours(rs.getInt("credit_hours"));
        // grade and gradePoint are not stored in the courses table;
        // they are populated by ResultDAO when reading student_course_results.
        c.setGrade("");
        c.setGradePoint(0.0);
        return c;
    }

    public List<Course> findAll() {
        String sql = "SELECT * FROM courses ORDER BY course_id";
        List<Course> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("CourseDAO.findAll failed", e);
        }
        return list;
    }

    public Optional<Course> findById(Long id) {
        String sql = "SELECT * FROM courses WHERE course_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id.intValue());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("CourseDAO.findById failed", e);
        }
        return Optional.empty();
    }

    public Optional<Course> findByCourseCode(String courseCode) {
        String sql = "SELECT * FROM courses WHERE course_code = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, courseCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("CourseDAO.findByCourseCode failed", e);
        }
        return Optional.empty();
    }
}
