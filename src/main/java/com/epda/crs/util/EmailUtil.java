package com.epda.crs.util;

import com.epda.crs.dao.EmailNotificationDAO;
import com.epda.crs.model.EmailNotification;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

public final class EmailUtil {

    private static final String SMTP_HOST  = "smtp.gmail.com";
    private static final int    SMTP_PORT  = 587;
    private static final String USERNAME   = "yixian908@gmail.com";
    private static final String PASSWORD   = "vlwn lfpf sizf fvgg";
    private static final String TEST_MODE  = "true";
    private static final String TEST_EMAIL = "yixian908@gmail.com";

    private EmailUtil() {}

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Sends an email via Gmail SMTP and logs the result to email_notifications.
     * Never throws — calling services must not be interrupted by mail failures.
     */
    public static void sendEmail(String recipient, String subject, String body) {
        String actualRecipient = TEST_MODE.equals("true") ? TEST_EMAIL : recipient;
        Session session = buildSession();
        String status = "SENT";
        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(USERNAME));
            msg.setRecipient(Message.RecipientType.TO, new InternetAddress(actualRecipient));
            msg.setSubject(subject);
            msg.setText(body);
            Transport.send(msg);
            System.out.printf("[EmailUtil] Sent → %s | %s%n", recipient, subject);
        } catch (Exception e) {
            status = "FAILED";
            System.err.printf("[EmailUtil] Failed to send to %s: %s%n", recipient, e.getMessage());
        }
        logToDb(recipient, subject, body, status);
    }

    /**
     * Logs an email record to email_notifications without actually sending.
     * Useful for testing or environments where SMTP is unavailable.
     */
    public static void logEmailOnly(String recipient, String subject, String body) {
        System.out.printf("[EmailUtil] Log-only → %s | %s%n", recipient, subject);
        logToDb(recipient, subject, body, "SENT");
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private static Session buildSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            String.valueOf(SMTP_PORT));
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });
    }

    private static void logToDb(String recipient, String subject, String body, String status) {
        try {
            EmailNotification n = new EmailNotification();
            n.setRecipient(recipient);
            n.setSubject(subject);
            n.setBody(body);
            n.setStatus(status);
            new EmailNotificationDAO().save(n);
        } catch (Exception e) {
            System.err.printf("[EmailUtil] DB log failed: %s%n", e.getMessage());
        }
    }
}
