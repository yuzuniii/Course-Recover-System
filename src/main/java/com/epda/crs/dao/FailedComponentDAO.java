package com.epda.crs.dao;

import com.epda.crs.config.DBConnection;
import com.epda.crs.model.FailedComponent;
import jakarta.enterprise.context.Dependent;
import java.io.Serializable;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Dependent
public class FailedComponentDAO implements Serializable {

    public List<FailedComponent> findByStudentId(Long studentId) {
        String sql = "SELECT fc.* FROM failed_components fc " +
                     "JOIN student_course_results scr ON fc.result_id = scr.result_id " +
                     "WHERE scr.student_id = ?";
        List<FailedComponent> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("FailedComponentDAO.findByStudentId failed", e);
        }
        return list;
    }

    public void save(FailedComponent comp) {
        String sql = "INSERT INTO failed_components (result_id, component_name, component_score, pass_required) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, comp.getResultId());
            ps.setString(2, comp.getComponentName());
            ps.setDouble(3, comp.getComponentScore());
            ps.setDouble(4, comp.getPassRequired());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) comp.setComponentId(rs.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("FailedComponentDAO.save failed", e);
        }
    }

    public void update(FailedComponent comp) {
        String sql = "UPDATE failed_components SET component_name = ?, component_score = ?, pass_required = ? WHERE component_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, comp.getComponentName());
            ps.setDouble(2, comp.getComponentScore());
            ps.setDouble(3, comp.getPassRequired());
            ps.setLong(4, comp.getComponentId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("FailedComponentDAO.update failed", e);
        }
    }

    public void delete(Long componentId) {
        String sql = "DELETE FROM failed_components WHERE component_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, componentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("FailedComponentDAO.delete failed", e);
        }
    }

    private FailedComponent mapRow(ResultSet rs) throws SQLException {
        FailedComponent fc = new FailedComponent();
        fc.setComponentId(rs.getLong("component_id"));
        fc.setResultId(rs.getInt("result_id"));
        fc.setComponentName(rs.getString("component_name"));
        fc.setComponentScore(rs.getDouble("component_score"));
        fc.setPassRequired(rs.getDouble("pass_required"));
        return fc;
    }
}
