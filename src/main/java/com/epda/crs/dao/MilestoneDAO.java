package com.epda.crs.dao;

import com.epda.crs.config.DBConnection;
import com.epda.crs.enums.MilestoneStatus;
import com.epda.crs.model.Milestone;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import jakarta.ejb.Stateless;

@Stateless
public class MilestoneDAO {

    private Milestone mapRow(ResultSet rs) throws SQLException {
        Milestone m = new Milestone();
        m.setId((long) rs.getInt("milestone_id"));
        m.setRecoveryPlanId((long) rs.getInt("plan_id"));
        m.setTitle(rs.getString("title"));
        m.setDescription(rs.getString("description"));
        Date dueDate = rs.getDate("due_date");
        if (dueDate != null) m.setDueDate(dueDate.toLocalDate());
        MilestoneStatus status;
        try {
            status = MilestoneStatus.valueOf(rs.getString("status"));
        } catch (Exception e) {
            status = MilestoneStatus.PENDING;
        }
        m.setStatus(status);
        return m;
    }

    public List<Milestone> findByPlanId(Long planId) {
        String sql = "SELECT * FROM recovery_milestones WHERE plan_id = ? ORDER BY due_date";
        List<Milestone> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, planId.intValue());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("MilestoneDAO.findByPlanId failed", e);
        }
        return list;
    }

    /** Alias used by RecoveryService. */
    public List<Milestone> findByRecoveryPlanId(Long planId) {
        return findByPlanId(planId);
    }

    public void save(Milestone milestone) {
        String sql = "INSERT INTO recovery_milestones (plan_id, title, description, due_date, status) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, milestone.getRecoveryPlanId().intValue());
            ps.setString(2, milestone.getTitle());
            ps.setString(3, milestone.getDescription());
            ps.setDate(4, milestone.getDueDate() != null ? Date.valueOf(milestone.getDueDate()) : null);
            ps.setString(5, milestone.getStatus() != null ? milestone.getStatus().name() : MilestoneStatus.PENDING.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) milestone.setId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("MilestoneDAO.save failed", e);
        }
    }

    public void update(Milestone milestone) {
        String sql = "UPDATE recovery_milestones SET title = ?, description = ?, due_date = ?, status = ? " +
                     "WHERE milestone_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, milestone.getTitle());
            ps.setString(2, milestone.getDescription());
            ps.setDate(3, milestone.getDueDate() != null ? Date.valueOf(milestone.getDueDate()) : null);
            ps.setString(4, milestone.getStatus() != null ? milestone.getStatus().name() : MilestoneStatus.PENDING.name());
            ps.setInt(5, milestone.getId().intValue());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("MilestoneDAO.update failed", e);
        }
    }

    public void updateStatus(Long milestoneId, MilestoneStatus status) {
        String sql = "UPDATE recovery_milestones SET status = ? WHERE milestone_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, milestoneId.intValue());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("MilestoneDAO.updateStatus failed", e);
        }
    }
}
