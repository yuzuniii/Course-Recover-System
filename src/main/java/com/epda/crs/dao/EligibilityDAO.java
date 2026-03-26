package com.epda.crs.dao;

import com.epda.crs.config.DBConnection;
import com.epda.crs.dto.EligibilityDTO;
import jakarta.enterprise.context.Dependent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import jakarta.ejb.Stateless;

/**
 * DAO for the eligibility_records table.
 */
@Stateless
public class EligibilityDAO {

    private EligibilityDTO mapRow(ResultSet rs) throws SQLException {
        EligibilityDTO dto = new EligibilityDTO();
        dto.setStudentId(rs.getInt("student_id"));
        dto.setSemester(rs.getInt("semester"));
        dto.setYearOfStudy(rs.getInt("year_of_study"));
        dto.setCgpa(rs.getDouble("cgpa"));
        dto.setFailedCourseCount(rs.getInt("failed_count"));
        dto.setEligible(rs.getBoolean("is_eligible"));
        dto.setReason(rs.getString("reason"));
        return dto;
    } // <-- FIXED: Added this brace to close mapRow() properly

    public java.util.Map<String, Long> getEligibilityStatusCounts() {
        String sql = "SELECT is_eligible, COUNT(*) FROM eligibility_records GROUP BY is_eligible";
        java.util.Map<String, Long> counts = new java.util.HashMap<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String status = rs.getBoolean(1) ? "Eligible" : "Not Eligible";
                counts.put(status, rs.getLong(2));
            }
        } catch (SQLException e) {
            throw new RuntimeException("EligibilityDAO.getEligibilityStatusCounts failed", e);
        }
        return counts;
    } // <-- FIXED: Removed the extra brace that was here closing the class early

    public void save(EligibilityDTO dto) {
        String sql = "INSERT INTO eligibility_records " +
                     "(student_id, semester, year_of_study, cgpa, failed_count, is_eligible, reason) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dto.getStudentId());
            ps.setInt(2, dto.getSemester());
            ps.setInt(3, dto.getYearOfStudy());
            ps.setDouble(4, dto.getCgpa());
            ps.setInt(5, dto.getFailedCourseCount());
            ps.setBoolean(6, dto.isEligible());
            ps.setString(7, dto.getReason());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("EligibilityDAO.save failed", e);
        }
    }

    public List<EligibilityDTO> findByStudentId(int studentId) {
        String sql = "SELECT * FROM eligibility_records WHERE student_id = ? " +
                     "ORDER BY checked_at DESC";
        List<EligibilityDTO> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("EligibilityDAO.findByStudentId failed", e);
        }
        return list;
    }
}