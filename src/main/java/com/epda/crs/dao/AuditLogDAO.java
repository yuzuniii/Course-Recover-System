package com.epda.crs.dao;

import com.epda.crs.config.DBConnection;
import com.epda.crs.model.AuditLog;
import jakarta.enterprise.context.Dependent;

import java.io.Serializable;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public List<AuditLog> findByActionType(String actionPart) {
        String sql = "SELECT * FROM audit_logs WHERE action LIKE ? ORDER BY logged_at DESC";
        List<AuditLog> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + actionPart + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("AuditLogDAO.findByActionType failed", e);
        }
        return list;
    }

    public Map<String, Long> getComponentFailureCounts() {
        String sql = "SELECT component_name, COUNT(*) as count FROM failed_components GROUP BY component_name";
        Map<String, Long> counts = new java.util.HashMap<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                counts.put(rs.getString("component_name"), rs.getLong("count"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("AuditLogDAO.getComponentFailureCounts failed", e);
        }
        return counts;
    }

    public long countPassingResults() {
        String sql = "SELECT COUNT(*) FROM student_course_results WHERE grade IN ('A', 'B', 'C', 'D')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            throw new RuntimeException("AuditLogDAO.countPassingResults failed", e);
        }
        return 0;
    }

    private AuditLog mapRow(ResultSet rs) throws SQLException {
        AuditLog log = new AuditLog();
        log.setId(rs.getLong("log_id"));
        log.setActionType(rs.getString("action"));
        log.setEntityName(rs.getString("entity_type"));
        long entityId = rs.getLong("entity_id");
        log.setEntityId(rs.wasNull() ? 0L : entityId);
        log.setDetails(rs.getString("description"));
        // prefer the joined username; falls back to "System" if no user row
        String username = rs.getString("username");
        log.setActorUsername((username != null && !username.isEmpty()) ? username : "System");
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

    public void saveWithUserId(Long userId, AuditLog log) {
        String sql = "INSERT INTO audit_logs (user_id, action, entity_type, entity_id, description) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId != null) ps.setInt(1, userId.intValue());
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
            throw new RuntimeException("AuditLogDAO.saveWithUserId failed", e);
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
     * Fetches activity counts for the last 7 days grouped by date and action type category.
     * Categories: 
     * - 'AUTH' for LOGIN_SUCCESS, LOGIN_FAILED, LOGOUT
     * - 'DATA' for CREATE, UPDATE, ADD
     * - 'ALERT' for DELETE, FAIL, ERROR
     */
    public java.util.List<java.util.Map<String, Object>> getMultiLineUsageTrend() {
        String sql = 
            "SELECT DATE_FORMAT(logged_at, '%Y-%m-%d') as log_date, DATE_FORMAT(logged_at, '%a') as day_name, " +
            "       CASE " +
            "           WHEN action IN ('LOGIN_SUCCESS', 'LOGIN_FAILED', 'LOGOUT') THEN 'AUTH' " +
            "           WHEN action LIKE 'CREATE%' OR action LIKE 'UPDATE%' OR action LIKE 'ADD%' THEN 'DATA' " +
            "           WHEN action LIKE 'DELETE%' OR action LIKE 'FAIL%' OR action LIKE 'ERROR%' THEN 'ALERT' " +
            "           ELSE 'OTHER' " +
            "       END as action_category, " +
            "       COUNT(*) as count " +
            "FROM audit_logs " +
            "WHERE logged_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
            "GROUP BY log_date, day_name, action_category " +
            "ORDER BY log_date ASC, action_category ASC";
        
        java.util.List<java.util.Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                java.util.Map<String, Object> row = new java.util.HashMap<>();
                row.put("date", rs.getString("log_date"));
                row.put("day", rs.getString("day_name"));
                row.put("category", rs.getString("action_category"));
                row.put("count", rs.getInt("count"));
                list.add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException("AuditLogDAO.getMultiLineUsageTrend failed", e);
        }
        return list;
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
