package com.epda.crs.dao;

import com.epda.crs.config.DBConnection;
import com.epda.crs.model.RecoveryRecommendation;
import jakarta.enterprise.context.Dependent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Dependent
public class RecoveryRecommendationDAO {

    private RecoveryRecommendation mapRow(ResultSet rs) throws SQLException {
        RecoveryRecommendation r = new RecoveryRecommendation();
        r.setId(rs.getLong("rec_id"));
        r.setPlanId(rs.getLong("plan_id"));
        r.setRecommendation(rs.getString("recommendation"));
        long addedBy = rs.getLong("added_by");
        r.setAddedBy(rs.wasNull() ? null : addedBy);
        Timestamp ts = rs.getTimestamp("added_at");
        if (ts != null) r.setAddedAt(ts.toLocalDateTime());
        return r;
    }

    public List<RecoveryRecommendation> findByPlanId(int planId) {
        String sql = "SELECT * FROM recovery_recommendations WHERE plan_id = ? ORDER BY added_at";
        List<RecoveryRecommendation> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("RecoveryRecommendationDAO.findByPlanId failed", e);
        }
        return list;
    }

    public void save(RecoveryRecommendation rec) {
        String sql = "INSERT INTO recovery_recommendations (plan_id, recommendation, added_by) " +
                     "VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, rec.getPlanId());
            ps.setString(2, rec.getRecommendation());
            if (rec.getAddedBy() != null) ps.setLong(3, rec.getAddedBy());
            else                          ps.setNull(3, java.sql.Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) rec.setId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("RecoveryRecommendationDAO.save failed", e);
        }
    }

    public void update(RecoveryRecommendation rec) {
        String sql = "UPDATE recovery_recommendations SET recommendation = ? WHERE rec_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rec.getRecommendation());
            ps.setLong(2, rec.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("RecoveryRecommendationDAO.update failed", e);
        }
    }

    public void delete(long recId) {
        String sql = "DELETE FROM recovery_recommendations WHERE rec_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, recId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("RecoveryRecommendationDAO.delete failed", e);
        }
    }
}
