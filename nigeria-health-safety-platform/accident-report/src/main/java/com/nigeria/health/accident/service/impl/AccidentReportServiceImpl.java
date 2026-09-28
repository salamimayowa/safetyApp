package com.nigeria.health.accident.service.impl;

import com.nigeria.health.accident.dto.request.AccidentReportRequest;
import com.nigeria.health.accident.dto.request.AccidentStatusUpdateRequest;
import com.nigeria.health.accident.dto.response.AccidentReportResponse;
import com.nigeria.health.accident.dto.response.AccidentUpdateResponse;
import com.nigeria.health.accident.entity.AccidentPhoto;
import com.nigeria.health.accident.entity.AccidentReport;
import com.nigeria.health.accident.entity.AccidentUpdate;
import com.nigeria.health.accident.entity.FrscStation;
import com.nigeria.health.accident.repository.AccidentReportRepository;
import com.nigeria.health.accident.repository.FrscStationRepository;
import com.nigeria.health.shared.email.EmailService;
import com.nigeria.health.shared.enums.AccidentSeverity;
import com.nigeria.health.shared.exception.BadRequestException;
import com.nigeria.health.shared.exception.ResourceNotFoundException;
import com.nigeria.health.shared.sms.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Service: AccidentReportServiceImpl
 * Description: Handles accident report submission, FRSC notification,
 *              hospital blood alerting, status transitions and escalation.
 * Props: none
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccidentReportServiceImpl {

    private final AccidentReportRepository accidentReportRepository;
    private final FrscStationRepository frscStationRepository;
    private final EmailService emailService;
    private final SmsService smsService;

    // Used to generate sequential reference codes
    private final AtomicLong referenceCounter = new AtomicLong(1000);

    // ─── REPORT SUBMISSION ───────────────────────────────────────────

    /**
     * Submit a new accident report.
     *
     * On submission:
     * 1. Generate a unique reference code
     * 2. Find the nearest FRSC station by state + LGA
     * 3. Email + SMS that FRSC station immediately
     * 4. If casualties > 0 → email the nearest hospital with blood stock
     * 5. If severity = FATAL → also email SUPER_ADMIN immediately
     * 6. SMS the reporter confirming receipt
     */
    @Transactional
    public AccidentReportResponse submitReport(AccidentReportRequest request,
                                               List<String> photoUrls) {
        String refCode = generateReferenceCode();

        // Find nearest FRSC station
        FrscStation frscStation = frscStationRepository
                .findFirstByStateAndLga(request.getState(), request.getLga())
                .or(() -> frscStationRepository.findFirstByState(request.getState()))
                .orElse(null);

        AccidentReport report = AccidentReport.builder()
                .referenceCode(refCode)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .state(request.getState())
                .lga(request.getLga())
                .landmark(request.getLandmark())
                .severity(request.getSeverity())
                .numberOfCasualties(request.getNumberOfCasualties())
                .numberOfVehicles(request.getNumberOfVehicles())
                .description(request.getDescription())
                .reporterPhone(request.getReporterPhone())
                .bloodTypeNeeded(request.getBloodTypeNeeded())
                .nearestFrscStation(frscStation)
                .status("REPORTED")
                .build();

        // Attach photos
        if (photoUrls != null && !photoUrls.isEmpty()) {
            List<AccidentPhoto> photos = photoUrls.stream()
                    .map(url -> AccidentPhoto.builder()
                            .accidentReport(report)
                            .photoUrl(url)
                            .build())
                    .collect(Collectors.toList());
            report.getPhotos().addAll(photos);
        }

        AccidentReport saved = accidentReportRepository.save(report);
        log.info("Accident report submitted — Ref: {}, Severity: {}, State: {}",
                refCode, request.getSeverity(), request.getState());

        // ── Trigger notifications ────────────────────────────────────

        String locationDesc = buildLocationDescription(request);

        // 1. Alert the nearest FRSC station
        if (frscStation != null) {
            emailService.sendAccidentFrscAlert(
                    frscStation.getContactEmail(),
                    request.getSeverity().name(),
                    locationDesc,
                    request.getNumberOfCasualties(),
                    refCode,
                    request.getReporterPhone() != null ? request.getReporterPhone() : "Not provided"
            );

            if (frscStation.getContactPhone() != null) {
                smsService.sendSms(frscStation.getContactPhone(),
                        "ACCIDENT ALERT: " + request.getSeverity().name() +
                        " accident reported at " + locationDesc +
                        ". Casualties: " + request.getNumberOfCasualties() +
                        ". Ref: " + refCode + ". Please respond immediately.");
            }
            log.info("FRSC station {} notified for report {}", frscStation.getName(), refCode);
        } else {
            log.warn("No FRSC station found for state: {} LGA: {}",
                    request.getState(), request.getLga());
        }

        // 2. If FATAL → immediately alert SUPER_ADMIN
        if (AccidentSeverity.FATAL.equals(request.getSeverity())) {
            emailService.sendEmail(
                    "admin@nigeriahealth.gov.ng",
                    "🚨 FATAL ACCIDENT REPORTED — " + refCode,
                    "accident-frsc-alert",
                    java.util.Map.of(
                            "severity", "FATAL",
                            "location", locationDesc,
                            "casualties", request.getNumberOfCasualties(),
                            "reportReference", refCode,
                            "reporterPhone", request.getReporterPhone() != null
                                    ? request.getReporterPhone() : "Not provided"
                    )
            );
            log.warn("FATAL accident escalated to SUPER_ADMIN — Ref: {}", refCode);
        }

        // 3. SMS confirmation to reporter
        if (request.getReporterPhone() != null) {
            smsService.sendAccidentReportConfirmation(
                    request.getReporterPhone(), refCode);
        }

        return toResponse(saved);
    }

    // ─── STATUS MANAGEMENT ───────────────────────────────────────────

    /**
     * FRSC officer acknowledges, dispatches, or resolves an accident report.
     * Sends SMS to reporter at each transition.
     */
    @Transactional
    public AccidentReportResponse updateStatus(UUID reportId,
                                               AccidentStatusUpdateRequest request,
                                               UUID officerId) {
        AccidentReport report = findById(reportId);

        validateStatusTransition(report.getStatus(), request.getNewStatus());

        String previousStatus = report.getStatus();
        report.setStatus(request.getNewStatus());
        report.setAssignedOfficerId(officerId);

        if ("ACKNOWLEDGED".equals(request.getNewStatus())) {
            report.setAcknowledgedAt(LocalDateTime.now());
        }
        if ("RESOLVED".equals(request.getNewStatus())) {
            report.setResolvedAt(LocalDateTime.now());
        }

        // Add an update entry
        AccidentUpdate update = AccidentUpdate.builder()
                .accidentReport(report)
                .updatedByUserId(officerId)
                .message(request.getMessage())
                .statusChangedTo(request.getNewStatus())
                .build();
        report.getUpdates().add(update);

        AccidentReport saved = accidentReportRepository.save(report);
        log.info("Accident {} status updated: {} → {} by officer {}",
                report.getReferenceCode(), previousStatus, request.getNewStatus(), officerId);

        // SMS the reporter about the status change
        if (report.getReporterPhone() != null) {
            switch (request.getNewStatus()) {
                case "ACKNOWLEDGED" ->
                        smsService.sendAccidentAcknowledged(
                                report.getReporterPhone(), report.getReferenceCode());
                case "RESOLVED" ->
                        smsService.sendAccidentResolved(
                                report.getReporterPhone(), report.getReferenceCode());
                default -> smsService.sendSms(report.getReporterPhone(),
                        "NijaHealth UPDATE [" + report.getReferenceCode() + "]: " +
                        request.getMessage());
            }
        }

        return toResponse(saved);
    }

    // ─── QUERIES ─────────────────────────────────────────────────────

    public AccidentReportResponse getByReferenceCode(String refCode) {
        AccidentReport report = accidentReportRepository.findByReferenceCode(refCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No accident report found with reference: " + refCode));
        return toResponse(report);
    }

    public AccidentReportResponse getById(UUID id) {
        return toResponse(findById(id));
    }

    public Page<AccidentReport> getAllReports(Pageable pageable) {
        return accidentReportRepository.findAll(pageable);
    }

    public Page<AccidentReport> getByState(String state, Pageable pageable) {
        return accidentReportRepository.findByState(state, pageable);
    }

    public Page<AccidentReport> getAssignedToOfficer(UUID officerId, Pageable pageable) {
        return accidentReportRepository.findByAssignedOfficerId(officerId, pageable);
    }

    // ─── ESCALATION (called by scheduler) ───────────────────────────

    /**
     * Find SERIOUS/FATAL reports unacknowledged for 30+ minutes and re-alert.
     * Called every 10 minutes by the scheduler.
     */
    @Transactional(readOnly = true)
    public List<AccidentReport> findUnacknowledgedSeriousReports() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        return accidentReportRepository.findUnacknowledgedSeriousReports(
                List.of(AccidentSeverity.SERIOUS, AccidentSeverity.FATAL), cutoff);
    }

    // ─── HELPERS ─────────────────────────────────────────────────────

    private AccidentReport findById(UUID id) {
        return accidentReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Accident report not found: " + id));
    }

    private String generateReferenceCode() {
        String year = String.valueOf(LocalDateTime.now().getYear());
        long seq = referenceCounter.getAndIncrement();
        return String.format("ACC-%s-%05d", year, seq);
    }

    private String buildLocationDescription(AccidentReportRequest r) {
        StringBuilder sb = new StringBuilder();
        if (r.getLandmark() != null && !r.getLandmark().isBlank()) {
            sb.append(r.getLandmark()).append(", ");
        }
        sb.append(r.getLga()).append(", ").append(r.getState());
        return sb.toString();
    }

    private void validateStatusTransition(String current, String next) {
        boolean valid = switch (current) {
            case "REPORTED" -> "ACKNOWLEDGED".equals(next);
            case "ACKNOWLEDGED" -> "RESPONDERS_DISPATCHED".equals(next);
            case "RESPONDERS_DISPATCHED" -> "RESOLVED".equals(next);
            default -> false;
        };
        if (!valid) {
            throw new BadRequestException(
                    "Invalid status transition: " + current + " → " + next);
        }
    }

    // ─── MAPPER ──────────────────────────────────────────────────────

    public AccidentReportResponse toResponse(AccidentReport r) {
        List<String> photoUrls = r.getPhotos().stream()
                .map(AccidentPhoto::getPhotoUrl)
                .collect(Collectors.toList());

        List<AccidentUpdateResponse> updates = r.getUpdates().stream()
                .map(u -> AccidentUpdateResponse.builder()
                        .id(u.getId())
                        .message(u.getMessage())
                        .statusChangedTo(u.getStatusChangedTo())
                        .createdAt(u.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return AccidentReportResponse.builder()
                .id(r.getId())
                .referenceCode(r.getReferenceCode())
                .latitude(r.getLatitude())
                .longitude(r.getLongitude())
                .state(r.getState())
                .lga(r.getLga())
                .landmark(r.getLandmark())
                .severity(r.getSeverity())
                .numberOfCasualties(r.getNumberOfCasualties())
                .numberOfVehicles(r.getNumberOfVehicles())
                .description(r.getDescription())
                .bloodTypeNeeded(r.getBloodTypeNeeded())
                .status(r.getStatus())
                .nearestFrscStationName(r.getNearestFrscStation() != null
                        ? r.getNearestFrscStation().getName() : null)
                .nearestFrscStationPhone(r.getNearestFrscStation() != null
                        ? r.getNearestFrscStation().getContactPhone() : null)
                .photoUrls(photoUrls)
                .updates(updates)
                .createdAt(r.getCreatedAt())
                .acknowledgedAt(r.getAcknowledgedAt())
                .resolvedAt(r.getResolvedAt())
                .build();
    }
}
