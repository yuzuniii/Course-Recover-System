package com.epda.crs.dao;

import com.epda.crs.config.DBConnection;
import com.epda.crs.model.FailedComponent;
import jakarta.ejb.Stateless;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Stateless
public class RecoveryPlanComponentDAO {

    public void save(int planId, int componentId) {
        String sql = "INSERT INTO recovery_plan_components (plan_id, component_id, status) " +
                     "VALUES (?, ?, 'PENDING')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, planId);
            ps.setInt(2, componentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("RecoveryPlanComponentDAO.save failed", e);
        }
    }

    public List<FailedComponent> findByPlanId(int planId) {
        String sql = "SELECT fc.component_id, fc.result_id, fc.component_name, " +
                     "       fc.component_score, fc.pass_required " +
                     "FROM   recovery_plan_components rpc " +
                     "JOIN   failed_components fc ON rpc.component_id = fc.component_id " +
                     "WHERE  rpc.plan_id = ?";
        List<FailedComponent> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FailedComponent fc = new FailedComponent();
                    fc.setComponentId(rs.getInt("component_id"));
                    fc.setResultId(rs.getInt("result_id"));
                    fc.setComponentName(rs.getString("component_name"));
                    fc.setComponentScore(rs.getDouble("component_score"));
                    fc.setPassRequired(rs.getDouble("pass_required"));
                    list.add(fc);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("RecoveryPlanComponentDAO.findByPlanId failed", e);
        }
        return list;
    }
}
