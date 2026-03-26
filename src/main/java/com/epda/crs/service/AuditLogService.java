package com.epda.crs.service;

import com.epda.crs.dao.AuditLogDAO;
import com.epda.crs.model.AuditLog;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

@Stateless
public class AuditLogService {
    private static final AtomicLong COUNTER = new AtomicLong(100);
    @Inject private AuditLogDAO auditLogDAO;

    public List<AuditLog> getAuditLogs() { return auditLogDAO.findAll(); }

    public void logAction(String actorUsername, String actionType, String entityName, Long entityId, String details) {
        auditLogDAO.save(new AuditLog(COUNTER.incrementAndGet(), actorUsername, actionType, entityName, entityId, details, LocalDateTime.now()));
    }

    public void logAction(Long actorUserId, String actionType, String entityName, Long entityId, String details) {
        AuditLog log = new AuditLog();
        log.setActionType(actionType);
        log.setEntityName(entityName);
        log.setEntityId(entityId);
        log.setDetails(details);
        log.setCreatedAt(LocalDateTime.now());
        auditLogDAO.saveWithUserId(actorUserId, log);
    }
}
