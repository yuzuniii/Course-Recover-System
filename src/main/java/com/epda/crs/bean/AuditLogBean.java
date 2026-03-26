package com.epda.crs.bean;

import com.epda.crs.model.AuditLog;
import com.epda.crs.service.AuditLogService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

@Named
@ViewScoped
public class AuditLogBean implements Serializable {

    @Inject
    private AuditLogService auditLogService;

    private List<AuditLog> auditLogs;
    private List<AuditLog> filteredAuditLogs;
    private List<String> actionTypes;
    private List<String> actors;

    @PostConstruct
    public void init() {
        auditLogs = auditLogService.getAuditLogs();
        
        // Populate filter options
        actionTypes = auditLogs.stream()
                .map(AuditLog::getActionType)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        
        actors = auditLogs.stream()
                .map(AuditLog::getActorUsername)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public List<AuditLog> getAuditLogs() {
        return auditLogs;
    }

    public List<AuditLog> getFilteredAuditLogs() {
        return filteredAuditLogs;
    }

    public void setFilteredAuditLogs(List<AuditLog> filteredAuditLogs) {
        this.filteredAuditLogs = filteredAuditLogs;
    }

    public List<String> getActionTypes() {
        return actionTypes;
    }

    public List<String> getActors() {
        return actors;
    }
}
