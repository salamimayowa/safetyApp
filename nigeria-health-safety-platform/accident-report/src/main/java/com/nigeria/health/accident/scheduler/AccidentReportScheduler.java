package com.nigeria.health.accident.scheduler;

import com.nigeria.health.accident.entity.AccidentReport;
import com.nigeria.health.accident.service.impl.AccidentReportServiceImpl;
import com.nigeria.health.shared.email.EmailService;
import com.nigeria.health.shared.sms.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Scheduler: AccidentReportScheduler
 * Description: Background jobs for accident report escalation.
 *   Every 10 minutes: find SERIOUS/FATAL reports not acknowledged in 30+ minutes
 *   and send a second escalation alert to the FRSC station and SUPER_ADMIN.
 * Props: none
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccidentReportScheduler {

    private final AccidentReportServiceImpl accidentReportService;
    private final EmailService emailService;
    private final SmsService smsService;

    /**
     * Every 10 minutes: escalate unacknowledged serious/fatal accident reports.
     * If a SERIOUS or FATAL report was submitted 30+ minutes ago and is still
     * in REPORTED status, re-alert the FRSC station and SUPER_ADMIN.
     */
    @Scheduled(cron = "0 */10 * * * *")
    public void escalateUnacknowledgedReports() {
        log.info("SCHEDULER: Checking for unacknowledged serious/fatal accident reports...");

        List<AccidentReport> unacknowledged =
                accidentReportService.findUnacknowledgedSeriousReports();

        if (unacknowledged.isEmpty()) {
            log.info("SCHEDULER: All serious/fatal reports are acknowledged.");
            return;
        }

        log.warn("SCHEDULER: {} report(s) require escalation.", unacknowledged.size());

        unacknowledged.forEach(report -> {
            String location = report.getLga() + ", " + report.getState();

            // Re-alert FRSC station
            if (report.getNearestFrscStation() != null) {
                emailService.sendEmail(
                        report.getNearestFrscStation().getContactEmail(),
                        "⚠️ ESCALATION — Unacknowledged Accident [" + report.getReferenceCode() + "]",
                        "accident-frsc-alert",
                        Map.of(
                                "severity", report.getSeverity().name() + " (ESCALATED)",
                                "location", location,
                                "casualties", report.getNumberOfCasualties(),
                                "reportReference", report.getReferenceCode(),
                                "reporterPhone", report.getReporterPhone() != null
                                        ? report.getReporterPhone() : "Not provided"
                        )
                );

                if (report.getNearestFrscStation().getContactPhone() != null) {
                    smsService.sendSms(
                            report.getNearestFrscStation().getContactPhone(),
                            "ESCALATION ALERT: " + report.getSeverity().name() +
                            " accident Ref " + report.getReferenceCode() +
                            " at " + location + " has NOT been acknowledged. Respond IMMEDIATELY."
                    );
                }
            }

            // Alert SUPER_ADMIN
            emailService.sendEmail(
                    "admin@nigeriahealth.gov.ng",
                    "⚠️ ESCALATION: Unacknowledged " + report.getSeverity().name() +
                    " accident — " + report.getReferenceCode(),
                    "accident-frsc-alert",
                    Map.of(
                            "severity", report.getSeverity().name() + " (NOT ACKNOWLEDGED IN 30 MINS)",
                            "location", location,
                            "casualties", report.getNumberOfCasualties(),
                            "reportReference", report.getReferenceCode(),
                            "reporterPhone", report.getReporterPhone() != null
                                    ? report.getReporterPhone() : "Not provided"
                    )
            );

            log.warn("SCHEDULER: Escalation sent for report {} — severity: {} location: {}",
                    report.getReferenceCode(), report.getSeverity(), location);
        });
    }
}
