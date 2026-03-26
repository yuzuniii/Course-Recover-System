package com.epda.crs.dto;

import java.io.Serializable;

public class DashboardAnalyticsDTO implements Serializable {
    
    private long totalActivatedAccounts;
    private long totalEnrolledStudents;
    private long activeRecoveryPlans;
    private long overdueMilestones;

    public DashboardAnalyticsDTO() {
    }

    // --- Getters and Setters ---

    public long getTotalActivatedAccounts() {
        return totalActivatedAccounts;
    }

    public void setTotalActivatedAccounts(long totalActivatedAccounts) {
        this.totalActivatedAccounts = totalActivatedAccounts;
    }

    public long getTotalEnrolledStudents() {
        return totalEnrolledStudents;
    }

    public void setTotalEnrolledStudents(long totalEnrolledStudents) {
        this.totalEnrolledStudents = totalEnrolledStudents;
    }

    public long getActiveRecoveryPlans() {
        return activeRecoveryPlans;
    }

    public void setActiveRecoveryPlans(long activeRecoveryPlans) {
        this.activeRecoveryPlans = activeRecoveryPlans;
    }

    public long getOverdueMilestones() {
        return overdueMilestones;
    }

    public void setOverdueMilestones(long overdueMilestones) {
        this.overdueMilestones = overdueMilestones;
    }
}