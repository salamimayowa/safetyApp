package com.nigeria.health.bloodbank.scheduler;

import com.nigeria.health.bloodbank.entity.BloodRequest;
import com.nigeria.health.bloodbank.repository.BloodRequestRepository;
import com.nigeria.health.bloodbank.repository.DonorRepository;
import com.nigeria.health.bloodbank.repository.HospitalRepository;
import com.nigeria.health.shared.email.EmailService;
import com.nigeria.health.shared.enums.UrgencyLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler: BloodBankScheduler
 * Description: All background jobs for the blood bank module.
 *   1. Every 30 mins: escalate CRITICAL/URGENT requests unfulfilled for 2+ hours
 *   2. Every day 8am: notify donors who are eligible to donate again (56-day rule)
 *   3. Every hour: auto-expire blood requests older than 24 hours
 *   4. Every 30 mins: check for low stock and alert hospital admins
 * Props: none
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BloodBankScheduler {

    private final BloodRequestRepository bloodRequestRepository;
    private final DonorRepository donorRepository;
    private final HospitalRepository hospitalRepository;
    private final EmailService emailService;

    /**
     * Every 30 minutes: find CRITICAL/URGENT blood requests unfulfilled for 2+ hours.
     * Sends another broadcast email to all approved hospitals in the same state.
     */
    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    public void escalateCriticalBloodRequests() {
        log.info("SCHEDULER: Checking for unescalated critical blood requests...");

        LocalDateTime twoHoursAgo = LocalDateTime.now().minusHours(2);
        List<BloodRequest> urgentRequests = bloodRequestRepository
                .findUnfulfilledUrgentRequests(
                        List.of(UrgencyLevel.CRITICAL, UrgencyLevel.URGENT),
                        twoHoursAgo);

        if (urgentRequests.isEmpty()) {
            log.info("SCHEDULER: No critical/urgent blood requests need escalation.");
            return;
        }

        log.warn("SCHEDULER: {} request(s) require escalation.", urgentRequests.size());

        urgentRequests.forEach(request -> {
            String state = request.getRequestingHospital().getState();
            List<com.nigeria.health.bloodbank.entity.Hospital> hospitalsInState =
                    hospitalRepository.findAllApprovedInState(state);

            hospitalsInState.stream()
                    .filter(h -> !h.getId().equals(request.getRequestingHospital().getId()))
                    .forEach(hospital -> {
                        emailService.sendBloodRequestAlert(
                                hospital.getContactEmail(),
                                request.getBloodType().getDisplay(),
                                "ESCALATED — " + request.getUrgency().name(),
                                request.getRequestingHospital().getName(),
                                request.getUnitsNeeded(),
                                request.getRequestingHospital().getContactEmail()
                        );
                    });

            log.warn("SCHEDULER: Escalation sent for blood request {} — {} blood needed in {}",
                    request.getId(), request.getBloodType().getDisplay(), state);
        });
    }

    /**
     * Every day at 8:00 AM: find donors whose 56-day waiting period ends today.
     * Sends a personalised email reminding them they can donate again.
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional(readOnly = true)
    public void notifyEligibleDonors() {
        log.info("SCHEDULER: Checking donors eligible to donate today...");

        // 56 days ago from today = the date those donors last donated
        LocalDate eligibilityDate = LocalDate.now().minusDays(56);

        var eligibleDonors = donorRepository.findDonorsEligibleToday(eligibilityDate);

        if (eligibleDonors.isEmpty()) {
            log.info("SCHEDULER: No donors became eligible today.");
            return;
        }

        log.info("SCHEDULER: Sending eligibility reminder to {} donor(s).",
                eligibleDonors.size());

        eligibleDonors.forEach(donor -> {
            emailService.sendDonorEligibleEmail(
                    donor.getUser().getEmail(),
                    donor.getUser().getFullName(),
                    donor.getLastDonationDate().toString()
            );

            // Update donor eligibility status
            donor.setIsEligible(true);
            donor.setIneligibilityReason(null);
            donorRepository.save(donor);

            log.info("SCHEDULER: Eligibility restored for donor: {}",
                    donor.getUser().getEmail());
        });
    }

    /**
     * Every hour: auto-expire blood requests that have passed their 24-hour window.
     * Changes status from OPEN → EXPIRED so they don't clutter the active list.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireOldBloodRequests() {
        log.info("SCHEDULER: Checking for expired blood requests...");

        List<BloodRequest> expired = bloodRequestRepository
                .findExpiredRequests(LocalDateTime.now());

        if (!expired.isEmpty()) {
            expired.forEach(r -> r.setStatus("EXPIRED"));
            bloodRequestRepository.saveAll(expired);
            log.info("SCHEDULER: Expired {} blood request(s).", expired.size());
        }
    }
}
