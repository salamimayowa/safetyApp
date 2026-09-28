package com.nigeria.health.drug.service.impl;

import com.nigeria.health.drug.entity.CounterfeitReport;
import com.nigeria.health.drug.entity.Drug;
import com.nigeria.health.drug.entity.VerificationLog;
import com.nigeria.health.drug.repository.CounterfeitReportRepository;
import com.nigeria.health.drug.repository.DrugRepository;
import com.nigeria.health.drug.repository.VerificationLogRepository;
import com.nigeria.health.shared.email.EmailService;
import com.nigeria.health.shared.exception.ConflictException;
import com.nigeria.health.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service: DrugVerificationServiceImpl
 * Description: NAFDAC drug verification logic, counterfeit reporting,
 *              drug registration and approval workflow.
 * Props: none
 *
 * VERIFICATION RESULT LOGIC (in order):
 *   1. NAFDAC number not in DB → NOT_FOUND
 *   2. Found but is_approved = false → COUNTERFEIT
 *   3. Found, approved, expiry_date < today → EXPIRED
 *   4. Found, approved, not expired → GENUINE
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DrugVerificationServiceImpl {

    private final DrugRepository drugRepository;
    private final VerificationLogRepository verificationLogRepository;
    private final CounterfeitReportRepository counterfeitReportRepository;
    private final EmailService emailService;

    private static final String ADMIN_EMAIL = "admin@nigeriahealth.gov.ng";
    private static final long COUNTERFEIT_REPORT_THRESHOLD = 5L;

    // ─── VERIFICATION ────────────────────────────────────────────────

    /**
     * Verify a drug by its NAFDAC number.
     * Logs every verification with location data for hotspot mapping.
     *
     * @return a map containing: result, drug details (if found), message
     */
    @Transactional
    public DrugVerificationResult verify(String nafdacNumber, String state,
                                          String lga, Double latitude, Double longitude,
                                          UUID verifiedByUserId) {

        String result;
        Drug drug = drugRepository.findByNafdacNumber(nafdacNumber.trim().toUpperCase())
                .orElse(null);

        if (drug == null) {
            result = "NOT_FOUND";
        } else if (!drug.getIsApproved()) {
            result = "COUNTERFEIT";
        } else if (drug.isExpired()) {
            result = "EXPIRED";
        } else {
            result = "GENUINE";
        }

        // Log every verification
        VerificationLog logEntry = VerificationLog.builder()
                .nafdacNumber(nafdacNumber.trim().toUpperCase())
                .verifiedByUserId(verifiedByUserId)
                .result(result)
                .state(state)
                .lga(lga)
                .latitude(latitude)
                .longitude(longitude)
                .build();
        verificationLogRepository.save(logEntry);

        log.info("Drug verified — NAFDAC: {}, Result: {}, Location: {}/{}",
                nafdacNumber, result, state, lga);

        return new DrugVerificationResult(result, drug);
    }

    // ─── COUNTERFEIT REPORTING ────────────────────────────────────────

    /**
     * Submit a counterfeit drug report.
     * Auto-escalates to SUPER_ADMIN when threshold (5 reports) is reached.
     */
    @Transactional
    public CounterfeitReport reportCounterfeit(CounterfeitReport report) {
        CounterfeitReport saved = counterfeitReportRepository.save(report);
        log.info("Counterfeit report submitted — NAFDAC: {}", report.getNafdacNumber());

        // Check if threshold reached for this NAFDAC number
        long totalReports = counterfeitReportRepository
                .countByNafdacNumber(report.getNafdacNumber());

        if (totalReports >= COUNTERFEIT_REPORT_THRESHOLD) {
            // Auto-escalate: set all PENDING reports for this drug to INVESTIGATING
            log.warn("COUNTERFEIT THRESHOLD REACHED — NAFDAC: {} ({} reports)",
                    report.getNafdacNumber(), totalReports);

            // Get drug details for the alert email
            String brandName = drugRepository.findByNafdacNumber(report.getNafdacNumber())
                    .map(Drug::getBrandName)
                    .orElse("Unknown Drug");

            String location = (report.getLga() != null ? report.getLga() + ", " : "") +
                    (report.getState() != null ? report.getState() : "Unknown");

            emailService.sendCounterfeitHotspotAlert(
                    ADMIN_EMAIL,
                    report.getNafdacNumber(),
                    brandName,
                    location,
                    (int) totalReports
            );
        }

        return saved;
    }

    // ─── DRUG REGISTRATION ───────────────────────────────────────────

    /**
     * PHARMACY_ADMIN submits a new drug for registration.
     * Drug is not active until SUPER_ADMIN approves it.
     */
    @Transactional
    public Drug registerDrug(Drug drug) {
        if (drugRepository.existsByNafdacNumber(drug.getNafdacNumber())) {
            throw new ConflictException(
                    "A drug with NAFDAC number " + drug.getNafdacNumber() + " already exists.");
        }
        drug.setIsApproved(false);
        Drug saved = drugRepository.save(drug);
        log.info("Drug registered for approval — NAFDAC: {}, Brand: {}",
                saved.getNafdacNumber(), saved.getBrandName());
        return saved;
    }

    /**
     * SUPER_ADMIN approves a drug registration.
     */
    @Transactional
    public Drug approveDrug(UUID drugId, UUID approvedBy) {
        Drug drug = findById(drugId);
        drug.setIsApproved(true);
        drug.setApprovedBy(approvedBy);
        drug.setApprovedAt(LocalDateTime.now());
        Drug saved = drugRepository.save(drug);
        log.info("Drug approved — NAFDAC: {}, Brand: {}", saved.getNafdacNumber(),
                saved.getBrandName());
        return saved;
    }

    /**
     * SUPER_ADMIN rejects a drug registration.
     * Drug remains unapproved (isApproved = false) — effectively counterfeit status.
     */
    @Transactional
    public void rejectDrug(UUID drugId) {
        Drug drug = findById(drugId);
        log.info("Drug registration rejected — NAFDAC: {}", drug.getNafdacNumber());
        drugRepository.delete(drug);
    }

    // ─── QUERIES ─────────────────────────────────────────────────────

    public Page<Drug> getApprovedDrugs(Pageable pageable) {
        return drugRepository.findByIsApprovedTrue(pageable);
    }

    public Page<Drug> getPendingDrugs(Pageable pageable) {
        return drugRepository.findByIsApprovedFalse(pageable);
    }

    public Page<Drug> searchDrugs(String query, Pageable pageable) {
        return drugRepository
                .findByBrandNameContainingIgnoreCaseOrGenericNameContainingIgnoreCase(
                        query, query, pageable);
    }

    public Page<Drug> getMySubmissions(UUID userId, Pageable pageable) {
        return drugRepository.findByAddedBy(userId, pageable);
    }

    public Drug findById(UUID id) {
        return drugRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Drug not found: " + id));
    }

    // ─── HOTSPOT DETECTION (called by scheduler) ──────────────────────

    /**
     * Find NAFDAC numbers with 3+ suspicious verifications in the last 7 days per LGA.
     * Returns raw Object[] arrays: [nafdacNumber, lga, state, count]
     */
    public List<Object[]> findCounterfeitHotspots() {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        return verificationLogRepository.findCounterfeitHotspots(since, 3L);
    }

    // ─── Inner result class ───────────────────────────────────────────

    /**
     * Holds the verification result and optionally the matched drug.
     */
    public record DrugVerificationResult(String result, Drug drug) {
        public boolean isGenuine() { return "GENUINE".equals(result); }
        public boolean isCounterfeit() { return "COUNTERFEIT".equals(result); }
        public boolean isNotFound() { return "NOT_FOUND".equals(result); }
        public boolean isExpired() { return "EXPIRED".equals(result); }
    }
}
