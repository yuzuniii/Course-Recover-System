package com.epda.crs.dao;

import com.epda.crs.config.DBConnection;
import com.epda.crs.dto.EligibilityDTO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import jakarta.ejb.Stateless;

/**
 * DAO for the eligibility_records table.
 *
 * Schema:
 *   eligibility_id INT AUTO_INCREMENT PRIMARY KEY,
 *   student_id     INT NOT NULL,
 *   semester       INT NOT NULL,
 *   year_of_study  INT NOT NULL,
 *   cgpa           DECIMAL(4,2),
 *   failed_count   INT,
 *   is_eligible    BOOLEAN DEFAULT FALSE,
 *   reason         VARCHAR(255),
 *   checked_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 *   checked_by     INT  (FK to users.user_id)
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
    }

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
