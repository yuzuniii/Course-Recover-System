package com.epda.crs.bean;

import com.epda.crs.enums.MilestoneStatus;
import com.epda.crs.exception.ValidationException;
import com.epda.crs.model.Milestone;
import com.epda.crs.model.RecoveryPlan;
import com.epda.crs.service.RecoveryService;
import jakarta.inject.Inject;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named
@RequestScoped
public class MilestoneBean implements Serializable {

    @Inject
    private RecoveryService recoveryService;

    @Inject
    private LoginBean loginBean;

    private List<Milestone> milestones;
    private Milestone selectedMilestone;
    private Milestone newMilestone = new Milestone();
    private int selectedPlanId;

    // -----------------------------------------------------------------------
    // Actions
    // -----------------------------------------------------------------------

    private String getActor() {
        return (loginBean != null && loginBean.getCurrentUser() != null) 
                ? loginBean.getCurrentUser().getUsername() : "system";
    }

    public void loadMilestones() {
        try {
            milestones = recoveryService.findById(selectedPlanId)
                    .map(RecoveryPlan::getMilestones)
                    .orElse(new ArrayList<>());
        } catch (ValidationException e) {
            addError("Milestones", e.getMessage());
        } catch (Exception e) {
            addError("Milestones", "An unexpected error occurred");
        }
    }

    public void addMilestone() {
        try {
            newMilestone.setRecoveryPlanId((long) selectedPlanId);
            recoveryService.addMilestone(newMilestone, getActor());
            loadMilestones();
            newMilestone = new Milestone();
            addInfo("Milestone", "Milestone added successfully");
        } catch (ValidationException e) {
            addError("Milestone", e.getMessage());
        } catch (Exception e) {
            addError("Milestone", "An unexpected error occurred");
        }
    }

    public void updateMilestone() {
        try {
            recoveryService.updateMilestone(selectedMilestone, getActor());
            loadMilestones();
            addInfo("Milestone", "Milestone updated successfully");
        } catch (ValidationException e) {
            addError("Milestone", e.getMessage());
        } catch (Exception e) {
            addError("Milestone", "An unexpected error occurred");
        }
    }

    public void updateStatus(int milestoneId, MilestoneStatus status) {
        try {
            recoveryService.updateMilestoneStatus(milestoneId, status, getActor());
            loadMilestones();
            addInfo("Milestone", "Milestone status updated successfully");
        } catch (ValidationException e) {
            addError("Milestone", e.getMessage());
        } catch (Exception e) {
            addError("Milestone", "An unexpected error occurred");
        }
    }

    public void prepareNew() {
        newMilestone = new Milestone();
    }

    // -----------------------------------------------------------------------
    // Getters and setters
    // -----------------------------------------------------------------------

    public List<Milestone> getMilestones() { return milestones; }
    public void setMilestones(List<Milestone> milestones) { this.milestones = milestones; }

    public Milestone getSelectedMilestone() { return selectedMilestone; }
    public void setSelectedMilestone(Milestone selectedMilestone) { this.selectedMilestone = selectedMilestone; }

    public Milestone getNewMilestone() { return newMilestone; }
    public void setNewMilestone(Milestone newMilestone) { this.newMilestone = newMilestone; }

    public int getSelectedPlanId() { return selectedPlanId; }
    public void setSelectedPlanId(int selectedPlanId) { this.selectedPlanId = selectedPlanId; }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void addInfo(String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, summary, detail));
    }

    private void addError(String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, detail));
    }
}
