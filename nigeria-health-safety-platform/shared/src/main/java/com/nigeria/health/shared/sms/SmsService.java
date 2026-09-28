package com.nigeria.health.shared.sms;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * SMS delivery service using the Termii API (Nigerian-friendly, free tier available).
 *
 * All SMS sends are @Async — they never block the HTTP response.
 * Termii API docs: https://developers.termii.com/
 *
 * To get your API key:
 * 1. Sign up at https://termii.com
 * 2. Go to API Keys → copy your key
 * 3. Add to application.yml: app.termii.api-key: YOUR_KEY
 *
 * Nigerian phone format: +2348012345678 or 08012345678
 * Termii handles both formats.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private final RestTemplate restTemplate;

    @Value("${app.termii.api-key}")
    private String apiKey;

    @Value("${app.termii.sender-id}")
    private String senderId; // e.g. "NijaHealth" (max 11 chars)

    private static final String TERMII_URL = "https://api.ng.termii.com/api/sms/send";

    /**
     * Send an SMS to a Nigerian phone number.
     *
     * @param to      phone number (08012345678 or +2348012345678)
     * @param message SMS message body (max 160 chars for single SMS)
     */
    @Async
    public void sendSms(String to, String message) {
        try {
            // Normalise phone number to international format
            String phone = normalisePhone(to);

            Map<String, Object> body = new HashMap<>();
            body.put("api_key", apiKey);
            body.put("to", phone);
            body.put("from", senderId);
            body.put("sms", message);
            body.put("type", "plain");
            body.put("channel", "generic");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    TERMII_URL, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("SMS sent to: {} | Message preview: {}",
                        phone, message.substring(0, Math.min(50, message.length())));
            } else {
                log.error("Termii SMS failed | Status: {} | Body: {}",
                        response.getStatusCode(), response.getBody());
            }

        } catch (Exception e) {
            log.error("Failed to send SMS to: {} | Error: {}", to, e.getMessage(), e);
        }
    }

    // ─── Convenience SMS methods ─────────────────────────────────────

    /** SMS sent to accident reporter confirming their report was received. */
    @Async
    public void sendAccidentReportConfirmation(String phone, String reference) {
        sendSms(phone,
                "NijaHealth: Your accident report has been received. " +
                "Ref: " + reference + ". Emergency services have been notified. " +
                "You will receive updates as the situation is attended to.");
    }

    /** SMS to reporter when FRSC officer acknowledges the report. */
    @Async
    public void sendAccidentAcknowledged(String phone, String reference) {
        sendSms(phone,
                "NijaHealth UPDATE: FRSC unit has acknowledged your report " +
                reference + " and responders are being dispatched. Stay safe.");
    }

    /** SMS to reporter when accident scene is resolved. */
    @Async
    public void sendAccidentResolved(String phone, String reference) {
        sendSms(phone,
                "NijaHealth UPDATE: Accident report " + reference +
                " has been resolved. Thank you for helping save lives.");
    }

    /** SMS to donor confirming donation appointment. */
    @Async
    public void sendDonationAppointmentConfirmation(String phone, String hospitalName,
                                                      String dateTime) {
        sendSms(phone,
                "NijaHealth: Your blood donation appointment at " + hospitalName +
                " is confirmed for " + dateTime +
                ". Please eat well before coming. Thank you for saving lives!");
    }

    /** SMS reminder to donor 24hrs before appointment. */
    @Async
    public void sendDonationReminder(String phone, String hospitalName, String dateTime) {
        sendSms(phone,
                "NijaHealth REMINDER: Your blood donation appointment at " +
                hospitalName + " is TOMORROW at " + dateTime +
                ". Please eat a good meal before coming.");
    }

    /** OTP via SMS as backup when email delivery is slow. */
    @Async
    public void sendOtpSms(String phone, String otpCode) {
        sendSms(phone,
                "NijaHealth: Your verification code is " + otpCode +
                ". Valid for 10 minutes. Do NOT share this code with anyone.");
    }

    // ─── Helper ─────────────────────────────────────────────────────

    /**
     * Convert Nigerian local number to international format for Termii.
     * 08012345678 → +2348012345678
     */
    private String normalisePhone(String phone) {
        if (phone == null) return phone;
        phone = phone.trim().replaceAll("\\s+", "");
        if (phone.startsWith("0")) {
            return "+234" + phone.substring(1);
        }
        if (phone.startsWith("234")) {
            return "+" + phone;
        }
        return phone; // already in +234... format or unknown format
    }
}
