package com.epda.crs.dao;

import com.epda.crs.config.DBConnection;
import com.epda.crs.model.AuditLog;
import jakarta.enterprise.context.Dependent;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Dependent
/**
 * DAO for audit_logs (log_id, user_id, action, entity_type, entity_id, description, logged_at).
 */
public class AuditLogDAO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Looks up user_id for the given username. Returns null if not found. */
    private Integer resolveUserId(Connection conn, String username) throws SQLException {
        String sql = "SELECT user_id FROM users WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("user_id");
            }
        }
        return null;
    }

    private AuditLog mapRow(ResultSet rs) throws SQLException {
        AuditLog log = new AuditLog();
        log.setId(rs.getLong("log_id"));
        log.setActionType(rs.getString("action"));
        log.setEntityName(rs.getString("entity_type"));
        long entityId = rs.getLong("entity_id");
        log.setEntityId(rs.wasNull() ? 0L : entityId);
        log.setDetails(rs.getString("description"));
        // prefer the joined username; falls back to empty string if no user row
        String username = rs.getString("username");
        log.setActorUsername(username != null ? username : "");
        Timestamp ts = rs.getTimestamp("logged_at");
        if (ts != null) log.setCreatedAt(ts.toLocalDateTime());
        return log;
    }

    public void save(AuditLog log) {
        String sql = "INSERT INTO audit_logs (user_id, action, entity_type, entity_id, description) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            Integer userId = resolveUserId(conn, log.getActorUsername());
            if (userId != null) ps.setInt(1, userId);
            else                ps.setNull(1, java.sql.Types.INTEGER);
            ps.setString(2, log.getActionType());
            ps.setString(3, log.getEntityName());
            if (log.getEntityId() != null && log.getEntityId() != 0)
                ps.setLong(4, log.getEntityId());
            else
                ps.setNull(4, java.sql.Types.INTEGER);
            ps.setString(5, log.getDetails());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("AuditLogDAO.save failed", e);
        }
    }

    private static final String BASE_SELECT =
        "SELECT al.log_id, al.action, al.entity_type, al.entity_id, al.description, al.logged_at, " +
        "       u.username " +
        "FROM   audit_logs al " +
        "LEFT JOIN users u ON al.user_id = u.user_id ";

    public List<AuditLog> findAll() {
        String sql = BASE_SELECT + "ORDER BY al.logged_at DESC";
        List<AuditLog> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("AuditLogDAO.findAll failed", e);
        }
        return list;
    }

    public List<AuditLog> findRecentActivity(int limit) {
        String sql = BASE_SELECT + "ORDER BY al.logged_at DESC LIMIT ?";
        List<AuditLog> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("AuditLogDAO.findRecentActivity failed", e);
        }
        return list;
    }

    /**
     * Fetches activity counts for the last 7 days.
     * Returns a map of Day (e.g., "Mon") -> Count.
     */
    public java.util.Map<String, Integer> getUsageTrend() {
        String sql = "SELECT DATE_FORMAT(logged_at, '%a') as day_name, COUNT(*) as count " +
                     "FROM audit_logs " +
                     "WHERE logged_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
                     "GROUP BY DATE(logged_at), day_name " +
                     "ORDER BY DATE(logged_at) ASC";
        
        java.util.Map<String, Integer> trend = new java.util.LinkedHashMap<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                trend.put(rs.getString("day_name"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("AuditLogDAO.getUsageTrend failed", e);
        }
        return trend;
    }

    public List<AuditLog> findByUserId(int userId) {
        String sql = BASE_SELECT + "WHERE al.user_id = ? ORDER BY al.logged_at DESC";
        List<AuditLog> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("AuditLogDAO.findByUserId failed", e);
        }
        return list;
    }
}
