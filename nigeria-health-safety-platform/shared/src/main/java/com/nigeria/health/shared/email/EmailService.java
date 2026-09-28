package com.nigeria.health.shared.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * Central email service for the entire platform.
 *
 * All emails are:
 * - Sent asynchronously (@Async) so they never block the HTTP response
 * - Built from Thymeleaf HTML templates stored in resources/templates/emails/
 * - Logged at INFO level on success, ERROR on failure
 *
 * Usage:
 *   emailService.sendEmail(
 *       "recipient@email.com",
 *       "Subject Line",
 *       "template-name",           // filename without .html
 *       Map.of("userName", "John", "otpCode", "482910")
 *   );
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Send an HTML email using a Thymeleaf template.
     *
     * @param to           recipient email address
     * @param subject      email subject line
     * @param templateName filename of the template (without .html extension)
     * @param variables    map of variables to inject into the template
     */
    @Async
    public void sendEmail(String to, String subject, String templateName,
                          Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);

            String htmlContent = templateEngine.process(
                    "emails/" + templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "Nigeria Health & Safety Platform");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML

            mailSender.send(message);
            log.info("Email sent successfully to: {} | Subject: {}", to, subject);

        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send email to: {} | Error: {}", to, e.getMessage(), e);
        }
    }

    /**
     * Send an HTML email with a file attachment (e.g. PDF report).
     */
    @Async
    public void sendEmailWithAttachment(String to, String subject, String templateName,
                                        Map<String, Object> variables,
                                        byte[] attachmentBytes, String attachmentFilename) {
        try {
            Context context = new Context();
            context.setVariables(variables);

            String htmlContent = templateEngine.process(
                    "emails/" + templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "Nigeria Health & Safety Platform");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.addAttachment(attachmentFilename,
                    new org.springframework.core.io.ByteArrayResource(attachmentBytes));

            mailSender.send(message);
            log.info("Email with attachment sent to: {} | File: {}", to, attachmentFilename);

        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send email with attachment to: {} | Error: {}",
                    to, e.getMessage(), e);
        }
    }

    // ─── Convenience methods for common emails ──────────────────────

    /** Send OTP code for email verification or password reset. */
    @Async
    public void sendOtpEmail(String to, String userName, String otpCode,
                             String purpose, int expiryMinutes) {
        sendEmail(to, "Your Verification Code — Nigeria Health Platform",
                "otp-email",
                Map.of(
                        "userName", userName,
                        "otpCode", otpCode,
                        "purpose", purpose,
                        "expiryMinutes", expiryMinutes
                ));
    }

    /** Send welcome email after successful account verification. */
    @Async
    public void sendWelcomeEmail(String to, String userName, String role) {
        sendEmail(to, "Welcome to Nigeria Health & Safety Platform",
                "welcome-email",
                Map.of(
                        "userName", userName,
                        "role", role
                ));
    }

    /** Notify hospital admin that their hospital registration was approved. */
    @Async
    public void sendHospitalApprovedEmail(String to, String hospitalName, String adminName) {
        sendEmail(to, "Hospital Registration Approved ✅",
                "hospital-approved",
                Map.of(
                        "hospitalName", hospitalName,
                        "adminName", adminName
                ));
    }

    /** Notify hospital admin that blood stock for a type is critically low. */
    @Async
    public void sendLowStockAlert(String to, String hospitalName,
                                   String bloodType, int unitsLeft) {
        sendEmail(to, "⚠️ Low Blood Stock Alert — " + bloodType,
                "low-stock-alert",
                Map.of(
                        "hospitalName", hospitalName,
                        "bloodType", bloodType,
                        "unitsLeft", unitsLeft
                ));
    }

    /** Alert nearby hospitals of a critical blood request. */
    @Async
    public void sendBloodRequestAlert(String to, String bloodType, String urgency,
                                       String requestingHospital, int unitsNeeded,
                                       String contactEmail) {
        sendEmail(to, "🩸 URGENT Blood Request — " + bloodType,
                "blood-request-alert",
                Map.of(
                        "bloodType", bloodType,
                        "urgency", urgency,
                        "requestingHospital", requestingHospital,
                        "unitsNeeded", unitsNeeded,
                        "contactEmail", contactEmail
                ));
    }

    /** Notify donor they are eligible to donate again. */
    @Async
    public void sendDonorEligibleEmail(String to, String donorName, String lastDonationDate) {
        sendEmail(to, "You Can Donate Blood Again 🩸",
                "donor-eligible",
                Map.of(
                        "donorName", donorName,
                        "lastDonationDate", lastDonationDate
                ));
    }

    /** Alert FRSC station of a new accident report. */
    @Async
    public void sendAccidentFrscAlert(String to, String severity, String location,
                                       int casualties, String reportReference,
                                       String reporterPhone) {
        sendEmail(to, "🚨 ACCIDENT REPORTED — " + severity + " | Ref: " + reportReference,
                "accident-frsc-alert",
                Map.of(
                        "severity", severity,
                        "location", location,
                        "casualties", casualties,
                        "reportReference", reportReference,
                        "reporterPhone", reporterPhone
                ));
    }

    /** Notify hospital of casualties from nearby accident needing blood. */
    @Async
    public void sendAccidentHospitalAlert(String to, String hospitalName,
                                           int casualties, String bloodTypeNeeded,
                                           String accidentLocation) {
        sendEmail(to, "🏥 Emergency — Casualties Reported Near You",
                "accident-hospital-alert",
                Map.of(
                        "hospitalName", hospitalName,
                        "casualties", casualties,
                        "bloodTypeNeeded",
                              bloodTypeNeeded != null ? bloodTypeNeeded : "Unknown",
                        "accidentLocation", accidentLocation
                ));
    }

    /** Alert admin of counterfeit drug detections in a hotspot area. */
    @Async
    public void sendCounterfeitHotspotAlert(String to, String nafdacNumber,
                                             String brandName, String location,
                                             int reportCount) {
        sendEmail(to, "⚠️ Counterfeit Drug Hotspot Detected — " + brandName,
                "drug-counterfeit-alert",
                Map.of(
                        "nafdacNumber", nafdacNumber,
                        "brandName", brandName,
                        "location", location,
                        "reportCount", reportCount
                ));
    }
}
