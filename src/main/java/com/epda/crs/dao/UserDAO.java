package com.epda.crs.dao;

import jakarta.ejb.Stateless;
import com.epda.crs.config.DBConnection;
import com.epda.crs.enums.AccountStatus;
import com.epda.crs.enums.UserRole;
import com.epda.crs.model.User;
import jakarta.enterprise.context.Dependent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Stateless
public class UserDAO {

    // roles table stores 'ADMIN' for COURSE_ADMINISTRATOR
    private UserRole mapRole(String roleName) {
        if (roleName == null) return null;
        if (roleName.equalsIgnoreCase("ADMIN")) return UserRole.COURSE_ADMINISTRATOR;
        if (roleName.equalsIgnoreCase("ACADEMIC_OFFICER")) return UserRole.ACADEMIC_OFFICER;
        try { return UserRole.valueOf(roleName); } catch (IllegalArgumentException e) { return null; }
    }

    private AccountStatus mapStatus(String status) {
        try { return AccountStatus.valueOf(status); } catch (Exception e) { return AccountStatus.ACTIVE; }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId((long) rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setStatus(mapStatus(rs.getString("status")));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) user.setCreatedAt(ts.toLocalDateTime());
        // role comes from the join — may be null if user has no role row
        String roleName = rs.getString("role_name");
        user.setRole(mapRole(roleName));
        Timestamp lastLoginTs = rs.getTimestamp("last_login");
        if (lastLoginTs != null) user.setLastLogin(lastLoginTs.toLocalDateTime());
        return user;
    }

    private static final String BASE_SELECT =
        "SELECT u.user_id, u.username, u.password, u.full_name, u.email, u.status, u.last_login, u.created_at, " +
        "       r.role_name " +
        "FROM   users u " +
        "LEFT JOIN user_roles ur ON u.user_id = ur.user_id " +
        "LEFT JOIN roles      r  ON ur.role_id = r.role_id ";

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(BASE_SELECT);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.findAll failed", e);
        }
        return list;
    }

    public Optional<User> findByUsername(String username) {
        String sql = BASE_SELECT + "WHERE u.username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.findByUsername failed", e);
        }
        return Optional.empty();
    }

    public Optional<User> findById(Long id) {
        String sql = BASE_SELECT + "WHERE u.user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id.intValue());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.findById failed", e);
        }
        return Optional.empty();
    }

    public void save(User user) {
        String sql = "INSERT INTO users (username, password, full_name, email, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getStatus() != null ? user.getStatus().name() : AccountStatus.ACTIVE.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next() && user.getRole() != null) {
                    long newId = keys.getLong(1);
                    insertUserRole(conn, newId, user.getRole());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.save failed", e);
        }
    }

    public void update(User user) {
        String sql = "UPDATE users SET username = ?, full_name = ?, email = ?, status = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getFullName());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getStatus() != null ? user.getStatus().name() : AccountStatus.ACTIVE.name());
            ps.setInt(5, user.getId().intValue());
            ps.executeUpdate();
            
            // Update role if provided
            if (user.getRole() != null) {
                updateUserRole(conn, user.getId(), user.getRole());
            }
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.update failed", e);
        }
    }

    private void updateUserRole(Connection conn, long userId, UserRole role) throws SQLException {
        // Delete existing role and insert new one
        String deleteSql = "DELETE FROM user_roles WHERE user_id = ?";
        try (PreparedStatement deletePs = conn.prepareStatement(deleteSql)) {
            deletePs.setLong(1, userId);
            deletePs.executeUpdate();
        }
        insertUserRole(conn, userId, role);
    }

    public void setActiveStatus(Long userId, boolean active) {
        String status = active ? AccountStatus.ACTIVE.name() : AccountStatus.INACTIVE.name();
        String sql = "UPDATE users SET status = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, userId.intValue());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.setActiveStatus failed", e);
        }
    }

    private void insertUserRole(Connection conn, long userId, UserRole role) throws SQLException {
        // Map enum back to the role_name stored in the roles table
        String roleName = (role == UserRole.COURSE_ADMINISTRATOR) ? "ADMIN" : role.name();
        String findRole = "SELECT role_id FROM roles WHERE role_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(findRole)) {
            ps.setString(1, roleName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int roleId = rs.getInt("role_id");
                    String insert = "INSERT IGNORE INTO user_roles (user_id, role_id) VALUES (?, ?)";
                    try (PreparedStatement ins = conn.prepareStatement(insert)) {
                        ins.setLong(1, userId);
                        ins.setInt(2, roleId);
                        ins.executeUpdate();
                    }
                }
            }
        }
    }

    public void delete(Long userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.delete failed", e);
        }
    }

    public long countActiveUsers() {
        String sql = "SELECT COUNT(*) FROM users WHERE status = 'ACTIVE'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.countActiveUsers failed", e);
        }
        return 0;
    }

    public void updateLastLogin(Long userId) {
        String sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.updateLastLogin failed", e);
        }
    }
}
