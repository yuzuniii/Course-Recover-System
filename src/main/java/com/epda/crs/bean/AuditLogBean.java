package com.epda.crs.bean;

import com.epda.crs.model.AuditLog;
import com.epda.crs.service.AuditLogService;
import java.io.Serializable;
import java.util.List;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

@Named
@ViewScoped
public class AuditLogBean implements Serializable {
    
    @EJB
    private AuditLogService auditLogService;

    // 2. Store the list in a variable so it stays stable across the view
    private List<AuditLog> auditLogs;

    // 3. Fetch the data ONLY ONCE when the page loads
    @PostConstruct
    public void init() {
        auditLogs = auditLogService.getAuditLogs();
    }

    // 4. Return the stored list without re-querying the database
    public List<AuditLog> getAuditLogs() { 
        return auditLogs; 
    }
}
