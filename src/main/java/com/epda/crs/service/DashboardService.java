package com.epda.crs.service;

import com.epda.crs.dao.StudentDAO;
import com.epda.crs.dto.DashboardAnalyticsDTO;
import com.epda.crs.model.RecoveryPlan;
import jakarta.ejb.EJB;
import java.util.List;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

@Stateless
public class DashboardService {
    @EJB
    private RecoveryService recoveryService;

    @EJB
    private EligibilityService eligibilityService;

    @EJB
    private RecoveryRuleService recoveryRuleService;

    @Inject
    private StudentDAO studentDAO;

    public DashboardAnalyticsDTO getAnalytics() {
        List<RecoveryPlan> plans = recoveryService.getRecoveryPlans();
        DashboardAnalyticsDTO dto = new DashboardAnalyticsDTO();
        dto.setStudentsUnderRecovery((int) plans.stream().map(RecoveryPlan::getStudent).distinct().count());
        dto.setActivePlans((int) plans.stream().filter(recoveryRuleService::isPlanActive).count());
        dto.setOverdueMilestones((int) plans.stream().flatMap(plan -> plan.getMilestones().stream()).filter(recoveryRuleService::isMilestoneOverdue).count());
        dto.setEligibleStudents((int) studentDAO.findAll().stream().filter(eligibilityService::isEligible).count());
        dto.setNonEligibleStudents(studentDAO.findAll().size() - dto.getEligibleStudents());
        return dto;
    }
}
