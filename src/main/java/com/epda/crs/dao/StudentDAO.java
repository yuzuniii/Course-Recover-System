package com.epda.crs.dao;

import com.epda.crs.config.DBConnection;
import com.epda.crs.model.Student;
import jakarta.enterprise.context.Dependent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import jakarta.ejb.Stateless;

@Stateless
public class StudentDAO {

    private Student mapRow(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setId((long) rs.getInt("student_id"));
        s.setStudentNumber(rs.getString("student_code"));
        s.setFullName(rs.getString("name"));
        s.setProgramName(rs.getString("programme"));
        s.setYearOfStudy(rs.getInt("year_of_study"));
        s.setSemester(rs.getInt("current_semester"));
        s.setCgpa(rs.getDouble("cgpa"));
        // failedCourseCount is computed at runtime by ResultDAO
        s.setFailedCourseCount(0);
        return s;
    }

    public List<Student> findAll() {
        String sql = "SELECT * FROM students ORDER BY student_id";
        List<Student> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("StudentDAO.findAll failed", e);
        }
        return list;
    }

    public Optional<Student> findById(Long id) {
        String sql = "SELECT * FROM students WHERE student_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id.intValue());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("StudentDAO.findById failed", e);
        }
        return Optional.empty();
    }

    public Optional<Student> findByStudentCode(String studentCode) {
        String sql = "SELECT * FROM students WHERE student_code = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("StudentDAO.findByStudentCode failed", e);
        }
        return Optional.empty();
    }

    public void save(Student student) {
        String sql = "INSERT INTO students (student_code, name, programme, year_of_study, current_semester, cgpa) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, student.getStudentNumber());
            ps.setString(2, student.getFullName());
            ps.setString(3, student.getProgramName());
            ps.setInt(4, student.getYearOfStudy());
            ps.setInt(5, student.getSemester());
            ps.setDouble(6, student.getCgpa());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("StudentDAO.save failed", e);
        }
    }

    public long countAllStudents() {
        String sql = "SELECT COUNT(*) FROM students";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            throw new RuntimeException("StudentDAO.countAllStudents failed", e);
        }
        return 0;
    }

    public void update(Student student) {
        String sql = "UPDATE students SET student_code = ?, name = ?, programme = ?, " +
                     "year_of_study = ?, current_semester = ?, cgpa = ? WHERE student_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, student.getStudentNumber());
            ps.setString(2, student.getFullName());
            ps.setString(3, student.getProgramName());
            ps.setInt(4, student.getYearOfStudy());
            ps.setInt(5, student.getSemester());
            ps.setDouble(6, student.getCgpa());
            ps.setInt(7, student.getId().intValue());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("StudentDAO.update failed", e);
        }
    }

    public java.util.Map<String, Double> getAverageCgpaByMajor() {
        String sql = "SELECT programme, AVG(cgpa) FROM students GROUP BY programme";
        java.util.Map<String, Double> map = new java.util.HashMap<>();
        
        try (java.sql.Connection conn = com.epda.crs.config.DBConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {
             
            while (rs.next()) {
                String major = rs.getString(1);
                double avgCgpa = rs.getDouble(2);
                
                // Round to 2 decimal places for a cleaner chart
                avgCgpa = Math.round(avgCgpa * 100.0) / 100.0; 
                
                // If a student somehow has no major, label them "Unknown"
                map.put(major != null ? major : "Unknown", avgCgpa);
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("StudentDAO.getAverageCgpaByMajor failed", e);
        }
        return map;
    }
}
