package com.epda.crs.bean;

import com.epda.crs.dao.EmailNotificationDAO;
import com.epda.crs.model.EmailNotification;
import com.epda.crs.service.AuditLogService;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;

@Named
@SessionScoped
public class EmailLogBean implements Serializable {

    @EJB
    private AuditLogService auditLogService;

    private List<EmailNotification> emailLogs;
    private String filterStatus = "ALL";
    private EmailNotification selectedLog;
    private int totalCount;
    private int sentCount;
    private int failedCount;

    @PostConstruct
    public void init() {
        loadLogs();
    }

    public void loadLogs() {
        EmailNotificationDAO dao = new EmailNotificationDAO();
        try {
            emailLogs   = dao.findAll();
            totalCount  = dao.countAll();
            sentCount   = dao.countByStatus("SENT");
            failedCount = dao.countByStatus("FAILED");
        } catch (Exception e) {
            emailLogs = new java.util.ArrayList<>();
        }
    }

    public void filterByStatus() {
        EmailNotificationDAO dao = new EmailNotificationDAO();
        try {
            if (filterStatus == null || filterStatus.equals("ALL")) {
                emailLogs = dao.findAll();
            } else {
                emailLogs = dao.findByStatus(filterStatus);
            }
        } catch (Exception e) {
            emailLogs = new java.util.ArrayList<>();
        }
    }

    public void viewBody(EmailNotification log) {
        this.selectedLog = log;
    }

    // -----------------------------------------------------------------------
    // Getters and setters
    // -----------------------------------------------------------------------

    public List<EmailNotification> getEmailLogs() { return emailLogs; }
    public void setEmailLogs(List<EmailNotification> emailLogs) { this.emailLogs = emailLogs; }

    public String getFilterStatus() { return filterStatus; }
    public void setFilterStatus(String filterStatus) { this.filterStatus = filterStatus; }

    public EmailNotification getSelectedLog() { return selectedLog; }
    public void setSelectedLog(EmailNotification selectedLog) { this.selectedLog = selectedLog; }

    public int getTotalCount()  { return totalCount; }
    public int getSentCount()   { return sentCount; }
    public int getFailedCount() { return failedCount; }
}
