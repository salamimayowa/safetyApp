package com.nigeria.health.drug.scheduler;

import com.nigeria.health.drug.service.impl.DrugVerificationServiceImpl;
import com.nigeria.health.shared.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduler: DrugVerificationScheduler
 * Description: Daily background job that scans verification logs to detect
 *              geographic counterfeit drug hotspots by LGA.
 *              If the same drug gets 3+ suspicious results in one LGA within 7 days,
 *              an alert is sent to SUPER_ADMIN.
 * Props: none
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DrugVerificationScheduler {

    private final DrugVerificationServiceImpl drugService;
    private final EmailService emailService;

    private static final String ADMIN_EMAIL = "admin@nigeriahealth.gov.ng";

    /**
     * Every day at 7:00 AM: detect counterfeit drug hotspots.
     * Queries verification_logs for NAFDAC numbers with 3+ suspicious hits
     * in the same LGA in the last 7 days.
     */
    @Scheduled(cron = "0 0 7 * * *")
    public void detectCounterfeitHotspots() {
        log.info("SCHEDULER: Running counterfeit hotspot detection...");

        List<Object[]> hotspots = drugService.findCounterfeitHotspots();

        if (hotspots.isEmpty()) {
            log.info("SCHEDULER: No counterfeit hotspots detected today.");
            return;
        }

        log.warn("SCHEDULER: {} counterfeit hotspot(s) detected!", hotspots.size());

        hotspots.forEach(row -> {
            String nafdacNumber = (String) row[0];
            String lga          = (String) row[1];
            String state        = (String) row[2];
            long   count        = (long)   row[3];

            String location = lga + ", " + state;
            log.warn("HOTSPOT: NAFDAC {} — {} reports in {} (last 7 days)",
                    nafdacNumber, count, location);

            emailService.sendCounterfeitHotspotAlert(
                    ADMIN_EMAIL,
                    nafdacNumber,
                    "See NAFDAC number",
                    location,
                    (int) count
            );
        });
    }
}
