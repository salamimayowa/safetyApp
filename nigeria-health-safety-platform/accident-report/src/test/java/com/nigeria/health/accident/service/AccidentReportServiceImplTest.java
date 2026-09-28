package com.nigeria.health.accident.service;

import com.nigeria.health.accident.dto.request.AccidentReportRequest;
import com.nigeria.health.accident.dto.request.AccidentStatusUpdateRequest;
import com.nigeria.health.accident.dto.response.AccidentReportResponse;
import com.nigeria.health.accident.entity.AccidentReport;
import com.nigeria.health.accident.entity.FrscStation;
import com.nigeria.health.accident.repository.AccidentReportRepository;
import com.nigeria.health.accident.repository.FrscStationRepository;
import com.nigeria.health.accident.service.impl.AccidentReportServiceImpl;
import com.nigeria.health.shared.email.EmailService;
import com.nigeria.health.shared.enums.AccidentSeverity;
import com.nigeria.health.shared.exception.BadRequestException;
import com.nigeria.health.shared.sms.SmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AccidentReportServiceImpl.
 * Covers: report submission, FRSC notification, FATAL escalation,
 *         status transitions, and invalid transition rejection.
 */
@ExtendWith(MockitoExtension.class)
class AccidentReportServiceImplTest {

    @Mock private AccidentReportRepository accidentReportRepository;
    @Mock private FrscStationRepository frscStationRepository;
    @Mock private EmailService emailService;
    @Mock private SmsService smsService;

    @InjectMocks
    private AccidentReportServiceImpl accidentReportService;

    private FrscStation sampleStation;
    private AccidentReportRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleStation = FrscStation.builder()
                .id(UUID.randomUUID())
                .name("FRSC Lagos Command — Ikeja")
                .state("Lagos")
                .lga("Ikeja")
                .contactEmail("frsc.lagos@frsc.gov.ng")
                .contactPhone("07080396615")
                .officerInCharge("Sector Commander Adeyemi")
                .build();

        sampleRequest = new AccidentReportRequest();
        sampleRequest.setLatitude(6.6018);
        sampleRequest.setLongitude(3.3515);
        sampleRequest.setState("Lagos");
        sampleRequest.setLga("Ikeja");
        sampleRequest.setLandmark("Lagos-Ibadan Expressway KM 12");
        sampleRequest.setSeverity(AccidentSeverity.SERIOUS);
        sampleRequest.setNumberOfCasualties(2);
        sampleRequest.setNumberOfVehicles(3);
        sampleRequest.setDescription("Three vehicles involved, two passengers injured");
        sampleRequest.setReporterPhone("08055123456");
    }

    // ─── REPORT SUBMISSION TESTS ──────────────────────────────────────

    @Nested
    @DisplayName("Report Submission")
    class ReportSubmissionTests {

        @Test
        @DisplayName("Emails the nearest FRSC station on submission")
        void emailsFrscStation_onSubmission() {
            when(frscStationRepository.findFirstByStateAndLga("Lagos", "Ikeja"))
                    .thenReturn(Optional.of(sampleStation));
            when(accidentReportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            accidentReportService.submitReport(sampleRequest, List.of());

            verify(emailService, times(1)).sendAccidentFrscAlert(
                    eq("frsc.lagos@frsc.gov.ng"),
                    eq("SERIOUS"),
                    contains("Ikeja"),
                    eq(2),
                    contains("ACC-"),
                    eq("08055123456")
            );
        }

        @Test
        @DisplayName("SMSes the FRSC station contact phone on submission")
        void smsfrscStation_onSubmission() {
            when(frscStationRepository.findFirstByStateAndLga("Lagos", "Ikeja"))
                    .thenReturn(Optional.of(sampleStation));
            when(accidentReportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            accidentReportService.submitReport(sampleRequest, List.of());

            verify(smsService, times(1)).sendSms(
                    eq("07080396615"), contains("ACC-"));
        }

        @Test
        @DisplayName("SMSes the reporter to confirm receipt")
        void smsReporter_onSubmission() {
            when(frscStationRepository.findFirstByStateAndLga("Lagos", "Ikeja"))
                    .thenReturn(Optional.of(sampleStation));
            when(accidentReportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            accidentReportService.submitReport(sampleRequest, List.of());

            verify(smsService, times(1)).sendAccidentReportConfirmation(
                    eq("08055123456"), contains("ACC-"));
        }

        @Test
        @DisplayName("FATAL accident immediately emails SUPER_ADMIN in addition to FRSC")
        void fatalAccident_alertsSuperAdmin() {
            sampleRequest.setSeverity(AccidentSeverity.FATAL);
            sampleRequest.setNumberOfCasualties(3);

            when(frscStationRepository.findFirstByStateAndLga("Lagos", "Ikeja"))
                    .thenReturn(Optional.of(sampleStation));
            when(accidentReportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            accidentReportService.submitReport(sampleRequest, List.of());

            // FRSC station email
            verify(emailService, times(1)).sendAccidentFrscAlert(
                    eq("frsc.lagos@frsc.gov.ng"), any(), any(), any(), any(), any());

            // SUPER_ADMIN escalation email
            verify(emailService, times(1)).sendEmail(
                    eq("admin@nigeriahealth.gov.ng"),
                    contains("FATAL"),
                    any(),
                    any()
            );
        }

        @Test
        @DisplayName("Submission succeeds even when no FRSC station found for the area")
        void submissionSucceeds_withNoFrscStation() {
            when(frscStationRepository.findFirstByStateAndLga("Zamfara", "Talata-Mafara"))
                    .thenReturn(Optional.empty());
            when(frscStationRepository.findFirstByState("Zamfara"))
                    .thenReturn(Optional.empty());
            when(accidentReportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            sampleRequest.setState("Zamfara");
            sampleRequest.setLga("Talata-Mafara");

            // Should not throw — gracefully handles missing FRSC station
            assertThatCode(() ->
                accidentReportService.submitReport(sampleRequest, List.of())
            ).doesNotThrowAnyException();
        }
    }

    // ─── STATUS TRANSITION TESTS ──────────────────────────────────────

    @Nested
    @DisplayName("Status Transitions")
    class StatusTransitionTests {

        private AccidentReport buildReport(String status) {
            return AccidentReport.builder()
                    .id(UUID.randomUUID())
                    .referenceCode("ACC-2025-01001")
                    .status(status)
                    .state("Lagos")
                    .lga("Ikeja")
                    .severity(AccidentSeverity.SERIOUS)
                    .numberOfCasualties(2)
                    .reporterPhone("08055123456")
                    .nearestFrscStation(sampleStation)
                    .build();
        }

        @Test
        @DisplayName("REPORTED → ACKNOWLEDGED is a valid transition")
        void validTransition_reportedToAcknowledged() {
            AccidentReport report = buildReport("REPORTED");
            AccidentStatusUpdateRequest req = new AccidentStatusUpdateRequest();
            req.setNewStatus("ACKNOWLEDGED");
            req.setMessage("Unit dispatched from Ikeja station");

            when(accidentReportRepository.findById(report.getId()))
                    .thenReturn(Optional.of(report));
            when(accidentReportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AccidentReportResponse response =
                    accidentReportService.updateStatus(report.getId(), req, UUID.randomUUID());

            assertThat(response.getStatus()).isEqualTo("ACKNOWLEDGED");
            verify(smsService, times(1)).sendAccidentAcknowledged(
                    eq("08055123456"), eq("ACC-2025-01001"));
        }

        @Test
        @DisplayName("RESOLVED → ACKNOWLEDGED is an invalid transition and throws exception")
        void invalidTransition_resolvedToAcknowledged() {
            AccidentReport report = buildReport("RESOLVED");
            AccidentStatusUpdateRequest req = new AccidentStatusUpdateRequest();
            req.setNewStatus("ACKNOWLEDGED");
            req.setMessage("Trying to go back");

            when(accidentReportRepository.findById(report.getId()))
                    .thenReturn(Optional.of(report));

            assertThatThrownBy(() ->
                accidentReportService.updateStatus(report.getId(), req, UUID.randomUUID())
            ).isInstanceOf(BadRequestException.class)
             .hasMessageContaining("Invalid status transition");
        }

        @Test
        @DisplayName("REPORTED → RESOLVED is an invalid transition")
        void invalidTransition_reportedToResolved() {
            AccidentReport report = buildReport("REPORTED");
            AccidentStatusUpdateRequest req = new AccidentStatusUpdateRequest();
            req.setNewStatus("RESOLVED");
            req.setMessage("Skip straight to resolved");

            when(accidentReportRepository.findById(report.getId()))
                    .thenReturn(Optional.of(report));

            assertThatThrownBy(() ->
                accidentReportService.updateStatus(report.getId(), req, UUID.randomUUID())
            ).isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("RESOLVED status sends SMS confirmation to reporter")
        void resolvedStatus_smsesReporter() {
            AccidentReport report = buildReport("RESPONDERS_DISPATCHED");
            AccidentStatusUpdateRequest req = new AccidentStatusUpdateRequest();
            req.setNewStatus("RESOLVED");
            req.setMessage("Scene cleared, all casualties evacuated");

            when(accidentReportRepository.findById(report.getId()))
                    .thenReturn(Optional.of(report));
            when(accidentReportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            accidentReportService.updateStatus(report.getId(), req, UUID.randomUUID());

            verify(smsService, times(1)).sendAccidentResolved(
                    eq("08055123456"), eq("ACC-2025-01001"));
        }
    }
}
