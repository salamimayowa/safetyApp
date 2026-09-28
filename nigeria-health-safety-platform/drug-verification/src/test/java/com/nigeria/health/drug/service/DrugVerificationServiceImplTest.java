package com.nigeria.health.drug.service;

import com.nigeria.health.drug.entity.CounterfeitReport;
import com.nigeria.health.drug.entity.Drug;
import com.nigeria.health.drug.repository.CounterfeitReportRepository;
import com.nigeria.health.drug.repository.DrugRepository;
import com.nigeria.health.drug.repository.VerificationLogRepository;
import com.nigeria.health.drug.service.impl.DrugVerificationServiceImpl;
import com.nigeria.health.drug.service.impl.DrugVerificationServiceImpl.DrugVerificationResult;
import com.nigeria.health.shared.email.EmailService;
import com.nigeria.health.shared.enums.DrugCategory;
import com.nigeria.health.shared.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DrugVerificationServiceImpl.
 * Covers all 4 verification outcomes, counterfeit threshold escalation,
 * and drug registration conflict detection.
 */
@ExtendWith(MockitoExtension.class)
class DrugVerificationServiceImplTest {

    @Mock private DrugRepository drugRepository;
    @Mock private VerificationLogRepository verificationLogRepository;
    @Mock private CounterfeitReportRepository counterfeitReportRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private DrugVerificationServiceImpl drugService;

    private Drug genuineDrug;
    private Drug counterfeitDrug;
    private Drug expiredDrug;

    @BeforeEach
    void setUp() {
        genuineDrug = Drug.builder()
                .id(UUID.randomUUID())
                .brandName("Coartem")
                .genericName("Artemether/Lumefantrine")
                .nafdacNumber("A4-0141L")
                .manufacturer("Novartis")
                .category(DrugCategory.ANTIMALARIAL)
                .isApproved(true)
                .expiryDate(LocalDate.now().plusYears(2))
                .build();

        counterfeitDrug = Drug.builder()
                .id(UUID.randomUUID())
                .brandName("Fake Coartem")
                .genericName("Unknown")
                .nafdacNumber("X9-9999Z")
                .manufacturer("Unknown")
                .isApproved(false)  // flagged as counterfeit
                .expiryDate(LocalDate.now().plusYears(1))
                .build();

        expiredDrug = Drug.builder()
                .id(UUID.randomUUID())
                .brandName("Old Paracetamol")
                .genericName("Paracetamol")
                .nafdacNumber("A1-0020P")
                .manufacturer("May & Baker")
                .isApproved(true)
                .expiryDate(LocalDate.now().minusDays(1))  // expired yesterday
                .build();
    }

    // ─── VERIFICATION OUTCOME TESTS ───────────────────────────────────

    @Nested
    @DisplayName("Verification Results")
    class VerificationResultTests {

        @Test
        @DisplayName("Returns GENUINE for an approved, non-expired drug")
        void returnsGenuine_forValidApprovedDrug() {
            when(drugRepository.findByNafdacNumber("A4-0141L"))
                    .thenReturn(Optional.of(genuineDrug));
            when(verificationLogRepository.save(any())).thenReturn(null);

            DrugVerificationResult result = drugService.verify(
                    "A4-0141L", "Lagos", "Surulere", null, null, null);

            assertThat(result.result()).isEqualTo("GENUINE");
            assertThat(result.drug()).isNotNull();
            assertThat(result.drug().getBrandName()).isEqualTo("Coartem");
            assertThat(result.isGenuine()).isTrue();
        }

        @Test
        @DisplayName("Returns COUNTERFEIT for an unapproved drug in the DB")
        void returnsCounterfeit_forUnapprovedDrug() {
            when(drugRepository.findByNafdacNumber("X9-9999Z"))
                    .thenReturn(Optional.of(counterfeitDrug));
            when(verificationLogRepository.save(any())).thenReturn(null);

            DrugVerificationResult result = drugService.verify(
                    "X9-9999Z", "Kano", "Nassarawa", null, null, null);

            assertThat(result.result()).isEqualTo("COUNTERFEIT");
            assertThat(result.isCounterfeit()).isTrue();
        }

        @Test
        @DisplayName("Returns EXPIRED for an approved but expired drug")
        void returnsExpired_forExpiredDrug() {
            when(drugRepository.findByNafdacNumber("A1-0020P"))
                    .thenReturn(Optional.of(expiredDrug));
            when(verificationLogRepository.save(any())).thenReturn(null);

            DrugVerificationResult result = drugService.verify(
                    "A1-0020P", "Enugu", "Enugu North", null, null, null);

            assertThat(result.result()).isEqualTo("EXPIRED");
            assertThat(result.isExpired()).isTrue();
        }

        @Test
        @DisplayName("Returns NOT_FOUND when NAFDAC number is not in the database")
        void returnsNotFound_whenDrugDoesNotExist() {
            when(drugRepository.findByNafdacNumber("Z0-0000X"))
                    .thenReturn(Optional.empty());
            when(verificationLogRepository.save(any())).thenReturn(null);

            DrugVerificationResult result = drugService.verify(
                    "Z0-0000X", "Abuja", "Municipal", null, null, null);

            assertThat(result.result()).isEqualTo("NOT_FOUND");
            assertThat(result.drug()).isNull();
            assertThat(result.isNotFound()).isTrue();
        }

        @Test
        @DisplayName("Every verification call saves a log entry regardless of result")
        void alwaysLogsVerification() {
            when(drugRepository.findByNafdacNumber(any())).thenReturn(Optional.empty());
            when(verificationLogRepository.save(any())).thenReturn(null);

            drugService.verify("Z0-0000X", "Lagos", "Ikeja", 6.5, 3.4, null);

            verify(verificationLogRepository, times(1)).save(argThat(log ->
                    "Z0-0000X".equals(log.getNafdacNumber()) &&
                    "NOT_FOUND".equals(log.getResult()) &&
                    "Lagos".equals(log.getState())
            ));
        }
    }

    // ─── COUNTERFEIT REPORT THRESHOLD TESTS ──────────────────────────

    @Nested
    @DisplayName("Counterfeit Report Escalation")
    class CounterfeitEscalationTests {

        @Test
        @DisplayName("Sends admin alert when counterfeit reports reach threshold of 5")
        void sendsAlert_whenThresholdReached() {
            CounterfeitReport report = CounterfeitReport.builder()
                    .nafdacNumber("X9-9999Z")
                    .pharmacyName("Fake Pharmacy")
                    .state("Lagos")
                    .lga("Ikeja")
                    .description("Packaging looks different and pills are discoloured")
                    .build();

            when(counterfeitReportRepository.save(any())).thenReturn(report);
            // Simulate this being the 5th report
            when(counterfeitReportRepository.countByNafdacNumber("X9-9999Z")).thenReturn(5L);
            when(drugRepository.findByNafdacNumber("X9-9999Z"))
                    .thenReturn(Optional.of(counterfeitDrug));

            drugService.reportCounterfeit(report);

            verify(emailService, times(1)).sendCounterfeitHotspotAlert(
                    eq("admin@nigeriahealth.gov.ng"),
                    eq("X9-9999Z"),
                    any(),
                    any(),
                    eq(5)
            );
        }

        @Test
        @DisplayName("Does NOT send admin alert when reports are below threshold")
        void doesNotAlert_belowThreshold() {
            CounterfeitReport report = CounterfeitReport.builder()
                    .nafdacNumber("X9-9999Z")
                    .description("Suspicious packaging")
                    .build();

            when(counterfeitReportRepository.save(any())).thenReturn(report);
            // Only 2 reports — below threshold of 5
            when(counterfeitReportRepository.countByNafdacNumber("X9-9999Z")).thenReturn(2L);

            drugService.reportCounterfeit(report);

            verify(emailService, never()).sendCounterfeitHotspotAlert(
                    any(), any(), any(), any(), anyInt());
        }
    }

    // ─── DRUG REGISTRATION TESTS ──────────────────────────────────────

    @Nested
    @DisplayName("Drug Registration")
    class DrugRegistrationTests {

        @Test
        @DisplayName("Throws ConflictException when NAFDAC number already exists")
        void throwsConflict_whenNafdacNumberDuplicated() {
            when(drugRepository.existsByNafdacNumber("A4-0141L")).thenReturn(true);

            Drug newDrug = Drug.builder()
                    .nafdacNumber("A4-0141L")
                    .brandName("Another Coartem")
                    .build();

            assertThatThrownBy(() -> drugService.registerDrug(newDrug))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("A4-0141L");
        }

        @Test
        @DisplayName("New drug starts as unapproved regardless of input")
        void newDrug_startsUnapproved() {
            Drug newDrug = Drug.builder()
                    .nafdacNumber("B5-1234X")
                    .brandName("Amoxil")
                    .isApproved(true) // caller tries to pre-approve — must be overridden
                    .build();

            when(drugRepository.existsByNafdacNumber("B5-1234X")).thenReturn(false);
            when(drugRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Drug saved = drugService.registerDrug(newDrug);

            assertThat(saved.getIsApproved()).isFalse();
        }
    }
}
