package com.epda.crs.util;

import com.epda.crs.dao.EmailNotificationDAO;
import com.epda.crs.dto.AcademicReportDTO;
import com.epda.crs.model.EmailNotification;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

public final class EmailUtil {

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String USERNAME  = "yixian908@gmail.com";
    private static final String PASSWORD  = "yvvf hlcq uvss wtgv";
    private static final boolean TEST_MODE  = true;
    private static final String TEST_EMAIL  = "yixian908@gmail.com";

    private EmailUtil() {}

    // -----------------------------------------------------------------------
    // Core send methods
    // -----------------------------------------------------------------------

    public static void sendEmail(String recipient, String subject, String body) {
        sendEmailHtml(recipient, subject,
                "<html><body style=\"font-family:Arial,sans-serif;color:#2c2c2c;\">"
                + body.replace("\n", "<br>") + "</body></html>");
    }

    public static void sendEmailHtml(String recipient, String subject, String htmlBody) {
        System.err.println("[EmailUtil] ATTEMPT: to=" + recipient + " subject=" + subject);

        String actualRecipient = TEST_MODE ? TEST_EMAIL
                : (recipient != null && !recipient.isEmpty()) ? recipient : null;
        if (actualRecipient == null) {
            System.err.println("[EmailUtil] SKIPPED: no recipient and TEST_MODE=false");
            return;
        }

        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth",            "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required","true");
            props.put("mail.smtp.host",            SMTP_HOST);
            props.put("mail.smtp.port",            SMTP_PORT);
            props.put("mail.smtp.ssl.trust",       "*");
            props.put("mail.smtp.ssl.protocols",   "TLSv1.2");

            Session session = Session.getInstance(props);

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(USERNAME));
            message.setRecipient(Message.RecipientType.TO,
                    new InternetAddress(actualRecipient));
            message.setSubject(subject);
            message.setContent(htmlBody, "text/html; charset=utf-8");

            try (Transport transport = session.getTransport("smtp")) {
                transport.connect(SMTP_HOST, Integer.parseInt(SMTP_PORT),
                        USERNAME, PASSWORD.replace(" ", ""));
                transport.sendMessage(message, message.getAllRecipients());
            }

            System.err.println("[EmailUtil] SENT successfully to " + actualRecipient);

            EmailNotification notification = new EmailNotification();
            notification.setRecipient(actualRecipient);
            notification.setSubject(subject);
            notification.setBody(htmlBody);
            notification.setStatus("SENT");
            notification.setSentAt(LocalDateTime.now());
            new EmailNotificationDAO().save(notification);

        } catch (Exception e) {
            System.err.println("[EmailUtil] FAILED: " + e.getMessage());

            try {
                EmailNotification notification = new EmailNotification();
                notification.setRecipient(actualRecipient);
                notification.setSubject(subject);
                notification.setBody(htmlBody);
                notification.setStatus("FAILED");
                notification.setSentAt(LocalDateTime.now());
                new EmailNotificationDAO().save(notification);
            } catch (Exception logEx) {
                System.err.println("[EmailUtil] DB log failed: " + logEx.getMessage());
            }
        }
    }

    // -----------------------------------------------------------------------
    // HTML template builders
    // -----------------------------------------------------------------------

    public static String buildEligibilityEmailHtml(String studentName, boolean eligible,
            double cgpa, int failedCourses, int semester, int year, String reason) {

        String statusColor  = eligible ? "#27500a" : "#791f1f";
        String statusBg     = eligible ? "#eaf3de" : "#fcebeb";
        String statusLabel  = eligible ? "ELIGIBLE" : "NOT ELIGIBLE";
        String cgpaColor    = cgpa >= 2.0 ? "#27500a" : "#791f1f";
        String failColor    = failedCourses > 3 ? "#791f1f" : "#27500a";

        return wrapper(studentName,
                "<h2 style=\"margin:0 0 4px;font-size:20px;color:#1a1f2e;\">Eligibility Check Result</h2>"
                + "<p style=\"margin:0;color:#6b6b6b;font-size:14px;\">Semester " + semester + " &nbsp;&bull;&nbsp; Year " + year + "</p>",
                "<div style=\"text-align:center;padding:20px 0;\">"
                + "  <span style=\"display:inline-block;padding:10px 32px;border-radius:6px;"
                + "background:" + statusBg + ";color:" + statusColor + ";font-size:18px;"
                + "font-weight:700;letter-spacing:1px;\">" + statusLabel + "</span>"
                + "</div>"
                + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse;margin:16px 0;\">"
                + statRow("CGPA", String.format("%.2f", cgpa), cgpaColor)
                + statRow("Failed Courses", String.valueOf(failedCourses), failColor)
                + statRow("Semester", String.valueOf(semester), "#2c2c2c")
                + statRow("Year of Study", String.valueOf(year), "#2c2c2c")
                + "</table>"
                + "<div style=\"background:#f5f5f4;border-left:4px solid #1a1f2e;padding:12px 16px;"
                + "border-radius:0 4px 4px 0;margin-top:8px;\">"
                + "<p style=\"margin:0;font-size:13px;color:#2c2c2c;\"><strong>Reason:</strong> " + esc(reason) + "</p>"
                + "</div>"
                + (eligible
                   ? "<p style=\"margin-top:16px;font-size:14px;color:#2c2c2c;\">You have been enrolled in your failed courses for course recovery. Please check with your academic officer for further details.</p>"
                   : "<p style=\"margin-top:16px;font-size:14px;color:#2c2c2c;\">Please contact your academic officer to discuss your options and next steps.</p>")
        );
    }

    public static String buildRecoveryPlanEmailHtml(String studentName, String courseCode,
            String courseTitle, int attemptNumber, String status, String startDate, String endDate) {

        String attemptColor = attemptNumber == 1 ? "#27500a" : attemptNumber == 2 ? "#633806" : "#791f1f";
        String attemptBg    = attemptNumber == 1 ? "#eaf3de" : attemptNumber == 2 ? "#faeeda" : "#fcebeb";

        return wrapper(studentName,
                "<h2 style=\"margin:0 0 4px;font-size:20px;color:#1a1f2e;\">Recovery Plan Activated</h2>"
                + "<p style=\"margin:0;color:#6b6b6b;font-size:14px;\">" + esc(courseCode) + " &mdash; " + esc(courseTitle) + "</p>",
                "<div style=\"text-align:center;padding:20px 0;\">"
                + "  <span style=\"display:inline-block;padding:8px 28px;border-radius:6px;"
                + "background:" + attemptBg + ";color:" + attemptColor + ";font-size:16px;font-weight:700;\">"
                + "Attempt " + attemptNumber + "</span>"
                + "</div>"
                + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse;margin:16px 0;\">"
                + statRow("Course Code", esc(courseCode), "#2c2c2c")
                + statRow("Course Name", esc(courseTitle), "#2c2c2c")
                + statRow("Attempt No.", String.valueOf(attemptNumber), attemptColor)
                + statRow("Status", esc(status), "#2c2c2c")
                + statRow("Start Date", esc(startDate), "#2c2c2c")
                + statRow("End Date",   esc(endDate),   "#2c2c2c")
                + "</table>"
                + "<div style=\"background:#f5f5f4;border-left:4px solid #1a1f2e;padding:12px 16px;"
                + "border-radius:0 4px 4px 0;margin-top:8px;\">"
                + "<p style=\"margin:0;font-size:13px;color:#2c2c2c;\">Your recovery plan milestones will be set by your academic officer. "
                + "Please complete all milestones before the end date to successfully pass the course recovery.</p>"
                + "</div>"
        );
    }

    public static String buildReportEmailHtml(String studentName, String studentCode,
            String programme, int semester, int year, double cgpa, String standing,
            List<AcademicReportDTO.CourseResultRow> results) {

        String cgpaColor   = cgpa >= 3.0 ? "#27500a" : cgpa >= 2.0 ? "#633806" : "#791f1f";
        String cgpaBg      = cgpa >= 3.0 ? "#eaf3de" : cgpa >= 2.0 ? "#faeeda" : "#fcebeb";

        StringBuilder rows = new StringBuilder();
        if (results != null) {
            for (AcademicReportDTO.CourseResultRow r : results) {
                boolean failed = "F".equals(r.getGrade());
                rows.append("<tr>")
                    .append("<td style=\"padding:8px 10px;border-bottom:1px solid #e8e6e0;font-size:13px;\">")
                    .append(esc(r.getCourseCode())).append("</td>")
                    .append("<td style=\"padding:8px 10px;border-bottom:1px solid #e8e6e0;font-size:13px;\">")
                    .append(esc(r.getCourseName())).append("</td>")
                    .append("<td style=\"padding:8px 10px;border-bottom:1px solid #e8e6e0;font-size:13px;text-align:center;\">")
                    .append(r.getCreditHours()).append("</td>")
                    .append("<td style=\"padding:8px 10px;border-bottom:1px solid #e8e6e0;font-size:13px;text-align:center;"
                            + "color:" + (failed ? "#791f1f" : "#27500a") + ";font-weight:700;\">")
                    .append(esc(r.getGrade())).append("</td>")
                    .append("<td style=\"padding:8px 10px;border-bottom:1px solid #e8e6e0;font-size:13px;text-align:center;"
                            + "color:" + (failed ? "#791f1f" : "#27500a") + ";\">")
                    .append(String.format("%.2f", r.getGradePoint())).append("</td>")
                    .append("</tr>");
            }
        }

        String tableSection = "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"border-collapse:collapse;margin:16px 0;border:1px solid #e8e6e0;border-radius:6px;overflow:hidden;\">"
                + "<thead><tr style=\"background:#1a1f2e;\">"
                + "<th style=\"padding:10px;color:#fff;font-size:12px;text-align:left;\">Code</th>"
                + "<th style=\"padding:10px;color:#fff;font-size:12px;text-align:left;\">Course</th>"
                + "<th style=\"padding:10px;color:#fff;font-size:12px;text-align:center;\">Cr.Hrs</th>"
                + "<th style=\"padding:10px;color:#fff;font-size:12px;text-align:center;\">Grade</th>"
                + "<th style=\"padding:10px;color:#fff;font-size:12px;text-align:center;\">Points</th>"
                + "</tr></thead>"
                + "<tbody>" + rows + "</tbody></table>";

        return wrapper(studentName,
                "<h2 style=\"margin:0 0 4px;font-size:20px;color:#1a1f2e;\">Academic Report</h2>"
                + "<p style=\"margin:0;color:#6b6b6b;font-size:14px;\">Semester " + semester + " &nbsp;&bull;&nbsp; Year " + year + " &nbsp;&bull;&nbsp; " + esc(programme) + "</p>",
                "<div style=\"text-align:center;padding:20px 0;\">"
                + "  <span style=\"display:inline-block;padding:10px 32px;border-radius:6px;"
                + "background:" + cgpaBg + ";color:" + cgpaColor + ";font-size:20px;font-weight:700;\">"
                + "CGPA &nbsp;" + String.format("%.2f", cgpa) + "</span>"
                + "  <br><span style=\"display:inline-block;margin-top:8px;font-size:13px;color:#6b6b6b;\">"
                + esc(standing) + "</span>"
                + "</div>"
                + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse;margin:8px 0 16px;\">"
                + statRow("Student Code", esc(studentCode), "#2c2c2c")
                + statRow("Programme",    esc(programme),   "#2c2c2c")
                + statRow("Semester",     String.valueOf(semester), "#2c2c2c")
                + statRow("Year",         String.valueOf(year),     "#2c2c2c")
                + "</table>"
                + tableSection
                + "<p style=\"font-size:13px;color:#6b6b6b;margin-top:8px;\">Please contact your academic officer for the full printed report.</p>"
        );
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private static String wrapper(String studentName, String headerContent, String bodyContent) {
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"></head><body style=\"margin:0;padding:0;"
                + "background:#f0f0ef;font-family:Arial,Helvetica,sans-serif;\">"
                + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f0f0ef;padding:32px 0;\">"
                + "<tr><td align=\"center\"><table width=\"600\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"max-width:600px;background:#ffffff;border-radius:8px;overflow:hidden;"
                + "border:1px solid #e8e6e0;\">"

                // Header
                + "<tr><td style=\"background:#1a1f2e;padding:28px 32px;\">"
                + "<p style=\"margin:0;font-size:12px;color:#8a9bbf;letter-spacing:1px;text-transform:uppercase;\">Course Recovery System</p>"
                + "</td></tr>"

                // Subheader
                + "<tr><td style=\"padding:24px 32px 16px;border-bottom:1px solid #e8e6e0;\">"
                + headerContent
                + "</td></tr>"

                // Greeting
                + "<tr><td style=\"padding:24px 32px 0;\">"
                + "<p style=\"margin:0 0 16px;font-size:15px;color:#2c2c2c;\">Dear <strong>" + esc(studentName) + "</strong>,</p>"
                + bodyContent
                + "</td></tr>"

                // Footer
                + "<tr><td style=\"padding:24px 32px;border-top:1px solid #e8e6e0;margin-top:16px;\">"
                + "<p style=\"margin:0;font-size:12px;color:#6b6b6b;\">This is an automated message from the Course Recovery System. "
                + "Please do not reply to this email.</p>"
                + "</td></tr>"

                + "</table></td></tr></table></body></html>";
    }

    private static String statRow(String label, String value, String valueColor) {
        return "<tr>"
                + "<td style=\"padding:8px 0;border-bottom:1px solid #e8e6e0;font-size:13px;color:#6b6b6b;width:45%;\">" + label + "</td>"
                + "<td style=\"padding:8px 0;border-bottom:1px solid #e8e6e0;font-size:13px;color:" + valueColor + ";font-weight:600;\">" + value + "</td>"
                + "</tr>";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
