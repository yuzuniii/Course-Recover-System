package com.epda.crs.service;

import com.epda.crs.dao.AuditLogDAO;
import com.epda.crs.dao.EligibilityDAO;
import com.epda.crs.dao.MilestoneDAO;
import com.epda.crs.dao.RecoveryDAO;
import com.epda.crs.dao.StudentDAO;
import com.epda.crs.dao.UserDAO;
import com.epda.crs.dto.DashboardAnalyticsDTO;
import com.epda.crs.model.AuditLog;
import com.epda.crs.model.RecoveryPlan;
import java.util.List;
import java.util.Map;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

@Stateless
public class DashboardService {

    @Inject private UserDAO userDAO;
    @Inject private StudentDAO studentDAO;
    @Inject private RecoveryDAO recoveryDAO;
    @Inject private MilestoneDAO milestoneDAO;
    @Inject private AuditLogDAO auditLogDAO;
    @Inject private EligibilityDAO eligibilityDAO;

    public DashboardAnalyticsDTO getAnalytics() {
        DashboardAnalyticsDTO dto = new DashboardAnalyticsDTO();
        dto.setTotalActivatedAccounts(userDAO.countActiveUsers());
        dto.setTotalEnrolledStudents(studentDAO.countAllStudents());
        dto.setActiveRecoveryPlans(recoveryDAO.countActivePlans());
        dto.setOverdueMilestones(milestoneDAO.countOverdueMilestones());
        return dto;
    }

    public Map<String, Long> getEligibilityChartData() {
        return eligibilityDAO.getEligibilityStatusCounts();
    }

    public List<RecoveryPlan> getCompletedRecoveryPlans() {
        return recoveryDAO.findCompletedPlans();
    }

    public List<AuditLog> getRecentActivity() {
        return auditLogDAO.findRecentActivity(5);
    }

    public java.util.Map<String, Integer> getUsageTrend() {
        return auditLogDAO.getUsageTrend();
    }

    public Map<String, Double> getAverageCgpaByMajor() {
        return studentDAO.getAverageCgpaByMajor();
    }
}
