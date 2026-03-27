package com.epda.crs.dao;

import com.epda.crs.config.DBConnection;
import com.epda.crs.model.EmailNotification;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class EmailNotificationDAO {

    private EmailNotification mapRow(ResultSet rs) throws SQLException {
        EmailNotification n = new EmailNotification();
        n.setNotifId(rs.getInt("email_id"));
        n.setRecipient(rs.getString("recipient"));
        n.setSubject(rs.getString("subject"));
        n.setBody(rs.getString("message"));
        n.setStatus(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("sent_at");
        if (ts != null) n.setSentAt(ts.toLocalDateTime());
        return n;
    }

    public List<EmailNotification> findAll() {
        String sql = "SELECT * FROM email_notifications ORDER BY sent_at DESC";
        List<EmailNotification> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("EmailNotificationDAO.findAll failed", e);
        }
        return list;
    }

    public List<EmailNotification> findByStatus(String status) {
        String sql = "SELECT * FROM email_notifications WHERE status = ? ORDER BY sent_at DESC";
        List<EmailNotification> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("EmailNotificationDAO.findByStatus failed", e);
        }
        return list;
    }

    public void save(EmailNotification n) {
        String sql = "INSERT INTO email_notifications (recipient, subject, message, status) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, n.getRecipient());
            ps.setString(2, n.getSubject());
            ps.setString(3, n.getBody());
            ps.setString(4, n.getStatus());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("EmailNotificationDAO.save failed", e);
        }
    }

    public int countAll() {
        String sql = "SELECT COUNT(*) FROM email_notifications";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("EmailNotificationDAO.countAll failed", e);
        }
        return 0;
    }

    public int countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM email_notifications WHERE status = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("EmailNotificationDAO.countByStatus failed", e);
        }
        return 0;
    }
}
