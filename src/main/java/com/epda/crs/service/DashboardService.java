package com.epda.crs.service;

import com.epda.crs.dao.AuditLogDAO;
import com.epda.crs.dao.EligibilityDAO;
import com.epda.crs.dao.MilestoneDAO;
import com.epda.crs.dao.RecoveryDAO;
import com.epda.crs.dao.ResultDAO;
import com.epda.crs.dao.StudentDAO;
import com.epda.crs.dao.UserDAO;
import com.epda.crs.dto.DashboardAnalyticsDTO;
import com.epda.crs.model.AuditLog;
import com.epda.crs.model.RecoveryPlan;
import jakarta.ejb.EJB;
import java.util.List;
import java.util.Map;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

@Stateless
public class DashboardService {

    @Inject private UserDAO userDAO;
    @Inject private StudentDAO studentDAO;
    @Inject private ResultDAO resultDAO;
    @Inject private RecoveryDAO recoveryDAO;
    @Inject private MilestoneDAO milestoneDAO;
    @Inject private AuditLogDAO auditLogDAO;
    @Inject private EligibilityDAO eligibilityDAO;

    public List<java.util.Map<String, Object>> getDailyActionCounts() {
        String sql = "SELECT DATE(logged_at) as log_date, action as action_type, COUNT(*) as act_count " +
                     "FROM audit_logs " +
                     "GROUP BY DATE(logged_at), action " +
                     "ORDER BY log_date DESC";
        List<java.util.Map<String, Object>> results = new java.util.ArrayList<>();
        try (java.sql.Connection conn = com.epda.crs.config.DBConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                java.util.Map<String, Object> row = new java.util.HashMap<>();
                java.sql.Date sqlDate = rs.getDate("log_date");
                row.put("log_date", sqlDate != null ? sqlDate.toLocalDate() : null);
                row.put("action_type", rs.getString("action_type"));
                row.put("act_count", rs.getInt("act_count"));
                results.add(row);
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("DashboardService.getDailyActionCounts failed", e);
        }
        return results;
    }

    public List<com.epda.crs.model.Milestone> getAllMilestones() {
        String sql = "SELECT * FROM recovery_milestones ORDER BY due_date";
        List<com.epda.crs.model.Milestone> list = new java.util.ArrayList<>();
        try (java.sql.Connection conn = com.epda.crs.config.DBConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                com.epda.crs.model.Milestone m = new com.epda.crs.model.Milestone();
                m.setId((long) rs.getInt("milestone_id"));
                m.setTitle(rs.getString("title"));
                java.sql.Date dueDate = rs.getDate("due_date");
                if (dueDate != null) m.setDueDate(dueDate.toLocalDate());
                list.add(m);
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("DashboardService.getAllMilestones failed", e);
        }
        return list;
    }

    public List<AuditLog> getReportAuditLogs() {
        return auditLogDAO.findByActionType("REPORT");
    }

    public Map<String, Long> getComponentChartData() {
        Map<String, Long> counts = auditLogDAO.getComponentFailureCounts();
        counts.put("PASS", auditLogDAO.countPassingResults());
        return counts;
    }

    public String getCourseNameByResultId(Integer resultId) {
        String sql = "SELECT c.course_name FROM student_course_results scr JOIN courses c ON scr.course_id = c.course_id WHERE scr.result_id = ?";
        try (java.sql.Connection conn = com.epda.crs.config.DBConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, resultId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("course_name");
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("DashboardService.getCourseNameByResultId failed", e);
        }
        return "Unknown Course";
    }

    public DashboardAnalyticsDTO getAnalytics() {
        DashboardAnalyticsDTO dto = new DashboardAnalyticsDTO();
        dto.setTotalActivatedAccounts(userDAO.countActiveUsers());
        dto.setTotalEnrolledStudents(studentDAO.countAllStudents());
        dto.setActiveRecoveryPlans(recoveryDAO.countActivePlans());
        dto.setOverdueMilestones(milestoneDAO.countOverdueMilestones());
        return dto;
    }

    public java.util.Map<String, Long> getEligibilityChartData() {
        // Business Rule: Eligible ONLY if cgpa >= 2.0 AND failed courses <= 3
        java.util.List<com.epda.crs.model.Student> students = studentDAO.findAll();
        long eligibleCount = 0;
        long ineligibleCount = 0;

        for (com.epda.crs.model.Student s : students) {
            // Recompute failed count at runtime for accuracy
            int failedCount = resultDAO.countFailedCourses(s.getId().intValue());
            if (s.getCgpa() >= 2.0 && failedCount <= 3) {
                eligibleCount++;
            } else {
                ineligibleCount++;
            }
        }

        java.util.Map<String, Long> counts = new java.util.HashMap<>();
        counts.put("ELIGIBLE", eligibleCount);
        counts.put("NOT_ELIGIBLE", ineligibleCount);
        return counts;
    }

    public List<RecoveryPlan> getCompletedRecoveryPlans() {
        return recoveryDAO.findCompletedPlans();
    }

    public List<AuditLog> getRecentActivity() {
        return auditLogDAO.findRecentActivity(5);
    }

    public java.util.List<java.util.Map<String, Object>> getMultiLineUsageTrend() {
        return auditLogDAO.getMultiLineUsageTrend();
    }

    public Map<String, Double> getAverageCgpaByMajor() {
        return studentDAO.getAverageCgpaByMajor();
    }
}
