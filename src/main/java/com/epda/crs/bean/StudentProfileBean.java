package com.epda.crs.bean;

import com.epda.crs.dao.EligibilityDAO;
import com.epda.crs.dao.MilestoneDAO;
import com.epda.crs.dao.RecoveryDAO;
import com.epda.crs.dao.RecoveryPlanComponentDAO;
import com.epda.crs.dao.ResultDAO;
import com.epda.crs.dao.StudentDAO;
import com.epda.crs.dto.EligibilityDTO;
import com.epda.crs.enums.MilestoneStatus;
import com.epda.crs.model.FailedComponent;
import com.epda.crs.model.Milestone;
import com.epda.crs.model.RecoveryPlan;
import com.epda.crs.model.Student;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Named
@ViewScoped
public class StudentProfileBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject private StudentDAO studentDAO;
    @Inject private ResultDAO resultDAO;
    @Inject private RecoveryDAO recoveryDAO;
    @Inject private MilestoneDAO milestoneDAO;
    @Inject private EligibilityDAO eligibilityDAO;
    @Inject private RecoveryPlanComponentDAO planComponentDAO;

    private int studentId;
    private Student student;
    private List<ResultDAO.FullResult> results;
    private List<RecoveryPlan> recoveryPlans;
    private EligibilityDTO eligibilityStatus;
    private int totalMilestones;
    private int completedMilestones;
    private double progressPct;
    private Map<Long, List<FailedComponent>> planComponents = new HashMap<>();

    @PostConstruct
    public void init() {
        String param = FacesContext.getCurrentInstance()
                .getExternalContext()
                .getRequestParameterMap()
                .get("studentId");
        if (param != null && !param.isEmpty()) {
            try {
                studentId = Integer.parseInt(param);
            } catch (NumberFormatException ignored) {}
        }
        if (studentId > 0) {
            loadStudentData();
        }
    }

    private void loadStudentData() {
        student = studentDAO.findById((long) studentId).orElse(null);
        if (student == null) return;

        results = resultDAO.getFullResultsByStudentId(studentId);
        recoveryPlans = recoveryDAO.findByStudentId(studentId);

        totalMilestones = 0;
        completedMilestones = 0;
        planComponents = new HashMap<>();

        for (RecoveryPlan plan : recoveryPlans) {
            List<Milestone> milestones = milestoneDAO.findByPlanId(plan.getId());
            totalMilestones += milestones.size();
            completedMilestones += (int) milestones.stream()
                    .filter(m -> m.getStatus() == MilestoneStatus.DONE
                              || m.getStatus() == MilestoneStatus.COMPLETED)
                    .count();
            try {
                planComponents.put(plan.getId(),
                        planComponentDAO.findByPlanId(plan.getId().intValue()));
            } catch (Exception e) {
                planComponents.put(plan.getId(), new ArrayList<>());
            }
        }

        progressPct = totalMilestones > 0
                ? (completedMilestones * 100.0 / totalMilestones) : 0;

        List<EligibilityDTO> records = eligibilityDAO.findByStudentId(studentId);
        if (!records.isEmpty()) {
            eligibilityStatus = records.get(0);
        }
    }

    /** Returns 0-100 rounded for use in progress bar width and display. */
    public long getProgressPercentage() {
        return Math.round(progressPct);
    }

    /** Returns plan components for a specific plan (used in row expansion). */
    public List<FailedComponent> getPlanComponents(Long planId) {
        if (planComponents == null || planId == null) return new ArrayList<>();
        return planComponents.getOrDefault(planId, new ArrayList<>());
    }

    // -----------------------------------------------------------------------
    // Getters and setters
    // -----------------------------------------------------------------------

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public Student getStudent() { return student; }

    public List<ResultDAO.FullResult> getResults() { return results; }

    public List<RecoveryPlan> getRecoveryPlans() { return recoveryPlans; }

    public EligibilityDTO getEligibilityStatus() { return eligibilityStatus; }

    public int getTotalMilestones() { return totalMilestones; }

    public int getCompletedMilestones() { return completedMilestones; }
}
