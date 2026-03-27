package com.epda.crs.model;

import java.time.LocalDateTime;

public class EmailNotification {

    private int           notifId;
    private String        recipient;
    private String        subject;
    private String        body;
    private String        status;
    private LocalDateTime sentAt;

    public EmailNotification() {}

    public EmailNotification(int notifId, String recipient, String subject,
                             String body, String status, LocalDateTime sentAt) {
        this.notifId   = notifId;
        this.recipient = recipient;
        this.subject   = subject;
        this.body      = body;
        this.status    = status;
        this.sentAt    = sentAt;
    }

    public int           getNotifId()   { return notifId; }
    public void          setNotifId(int notifId) { this.notifId = notifId; }

    public String        getRecipient() { return recipient; }
    public void          setRecipient(String recipient) { this.recipient = recipient; }

    public String        getSubject()   { return subject; }
    public void          setSubject(String subject) { this.subject = subject; }

    public String        getBody()      { return body; }
    public void          setBody(String body) { this.body = body; }

    public String        getStatus()    { return status; }
    public void          setStatus(String status) { this.status = status; }

    public LocalDateTime getSentAt()    { return sentAt; }
    public void          setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}
