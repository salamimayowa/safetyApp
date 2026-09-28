package com.nigeria.health.bloodbank.service;

import com.nigeria.health.bloodbank.entity.*;
import com.nigeria.health.bloodbank.repository.*;
import com.nigeria.health.bloodbank.service.impl.BloodBankServiceImpl;
import com.nigeria.health.shared.email.EmailService;
import com.nigeria.health.shared.enums.BloodType;
import com.nigeria.health.shared.enums.UrgencyLevel;
import com.nigeria.health.shared.exception.BadRequestException;
import com.nigeria.health.shared.exception.ConflictException;
import com.nigeria.health.shared.sms.SmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BloodBankServiceImpl.
 * Covers: donor eligibility, proximity matching, blood request creation,
 *         hospital approval, and low stock alert logic.
 */
@ExtendWith(MockitoExtension.class)
class BloodBankServiceImplTest {

    @Mock private HospitalRepository hospitalRepository;
    @Mock private BloodStockRepository bloodStockRepository;
    @Mock private BloodRequestRepository bloodRequestRepository;
    @Mock private DonorRepository donorRepository;
    @Mock private DonationRepository donationRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;
    @Mock private SmsService smsService;

    @InjectMocks
    private BloodBankServiceImpl bloodBankService;

    private Hospital sampleHospital;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("Dr. Aminu Kano")
                .email("admin@luth.edu.ng")
                .phone("08012000001")
                .isVerified(true)
                .isActive(true)
                .build();

        sampleHospital = Hospital.builder()
                .id(UUID.randomUUID())
                .name("Lagos University Teaching Hospital")
                .state("Lagos")
                .lga("Surulere")
                .contactEmail("admin@luth.edu.ng")
                .contactPhone("08012000001")
                .adminUser(sampleUser)
                .isApproved(true)
                .build();
    }

    // ─── DONOR ELIGIBILITY TESTS ──────────────────────────────────────

    @Nested
    @DisplayName("Donor Eligibility")
    class DonorEligibilityTests {

        @Test
        @DisplayName("Donor is eligible when no previous donation exists")
        void eligible_whenNoPreviousDonation() {
            Donor donor = Donor.builder()
                    .id(UUID.randomUUID())
                    .user(sampleUser)
                    .bloodType(BloodType.O_POSITIVE)
                    .dateOfBirth(LocalDate.now().minusYears(25))
                    .weightKg(70.0)
                    .lastDonationDate(null)
                    .isEligible(true)
                    .build();

            boolean eligible = donor.checkEligibility();

            assertThat(eligible).isTrue();
            assertThat(donor.getIsEligible()).isTrue();
            assertThat(donor.getIneligibilityReason()).isNull();
        }

        @Test
        @DisplayName("Donor is eligible exactly 56 days after last donation")
        void eligible_afterExactly56Days() {
            Donor donor = Donor.builder()
                    .id(UUID.randomUUID())
                    .user(sampleUser)
                    .bloodType(BloodType.A_POSITIVE)
                    .dateOfBirth(LocalDate.now().minusYears(30))
                    .weightKg(65.0)
                    .lastDonationDate(LocalDate.now().minusDays(56))
                    .build();

            boolean eligible = donor.checkEligibility();

            assertThat(eligible).isTrue();
        }

        @Test
        @DisplayName("Donor is NOT eligible 55 days after last donation")
        void notEligible_before56Days() {
            Donor donor = Donor.builder()
                    .id(UUID.randomUUID())
                    .user(sampleUser)
                    .bloodType(BloodType.B_NEGATIVE)
                    .dateOfBirth(LocalDate.now().minusYears(28))
                    .weightKg(60.0)
                    .lastDonationDate(LocalDate.now().minusDays(55))
                    .build();

            boolean eligible = donor.checkEligibility();

            assertThat(eligible).isFalse();
            assertThat(donor.getIneligibilityReason()).contains("1 more day");
        }

        @Test
        @DisplayName("Donor is NOT eligible if weight is below 50kg")
        void notEligible_underweight() {
            Donor donor = Donor.builder()
                    .id(UUID.randomUUID())
                    .user(sampleUser)
                    .bloodType(BloodType.O_NEGATIVE)
                    .dateOfBirth(LocalDate.now().minusYears(22))
                    .weightKg(48.0)
                    .build();

            boolean eligible = donor.checkEligibility();

            assertThat(eligible).isFalse();
            assertThat(donor.getIneligibilityReason()).containsIgnoringCase("50kg");
        }

        @Test
        @DisplayName("Donor is NOT eligible if under 18 years old")
        void notEligible_underAge() {
            Donor donor = Donor.builder()
                    .id(UUID.randomUUID())
                    .user(sampleUser)
                    .bloodType(BloodType.AB_POSITIVE)
                    .dateOfBirth(LocalDate.now().minusYears(17))
                    .weightKg(60.0)
                    .build();

            boolean eligible = donor.checkEligibility();

            assertThat(eligible).isFalse();
            assertThat(donor.getIneligibilityReason()).containsIgnoringCase("18");
        }
    }

    // ─── PROXIMITY MATCHING TESTS ─────────────────────────────────────

    @Nested
    @DisplayName("Proximity Matching")
    class ProximityMatchingTests {

        @Test
        @DisplayName("Returns LGA-level results before state-level results")
        void prioritisesLgaResults() {
            Hospital lgaHospital = Hospital.builder()
                    .id(UUID.randomUUID()).name("Surulere General")
                    .state("Lagos").lga("Surulere").isApproved(true).build();

            Hospital stateHospital = Hospital.builder()
                    .id(UUID.randomUUID()).name("Island Hospital")
                    .state("Lagos").lga("Eti-Osa").isApproved(true).build();

            when(hospitalRepository.findHospitalsInLgaWithBlood("Lagos", "Surulere",
                    BloodType.O_POSITIVE)).thenReturn(List.of(lgaHospital));

            when(hospitalRepository.findHospitalsInStateWithBlood("Lagos",
                    BloodType.O_POSITIVE)).thenReturn(List.of(lgaHospital, stateHospital));

            List<Hospital> results = bloodBankService.findHospitalsWithBlood(
                    BloodType.O_POSITIVE, "Lagos", "Surulere");

            assertThat(results).hasSize(2);
            assertThat(results.get(0).getName()).isEqualTo("Surulere General");
            assertThat(results.get(1).getName()).isEqualTo("Island Hospital");
        }

        @Test
        @DisplayName("Returns empty list when no hospitals have required blood type")
        void returnsEmpty_whenNoMatch() {
            when(hospitalRepository.findHospitalsInLgaWithBlood(any(), any(), any()))
                    .thenReturn(List.of());
            when(hospitalRepository.findHospitalsInStateWithBlood(any(), any()))
                    .thenReturn(List.of());

            List<Hospital> results = bloodBankService.findHospitalsWithBlood(
                    BloodType.AB_NEGATIVE, "Sokoto", "Wamako");

            assertThat(results).isEmpty();
        }
    }

    // ─── BLOOD REQUEST TESTS ──────────────────────────────────────────

    @Nested
    @DisplayName("Blood Requests")
    class BloodRequestTests {

        @Test
        @DisplayName("CRITICAL request broadcasts to all hospitals in the same state")
        void criticalRequest_broadcastsToAllHospitals() {
            Hospital otherHospital = Hospital.builder()
                    .id(UUID.randomUUID()).name("National Hospital")
                    .state("Lagos").lga("Ikeja")
                    .contactEmail("admin@national.gov.ng")
                    .isApproved(true).build();

            BloodRequest request = BloodRequest.builder()
                    .requestingHospital(sampleHospital)
                    .bloodType(BloodType.B_POSITIVE)
                    .unitsNeeded(3)
                    .urgency(UrgencyLevel.CRITICAL)
                    .build();

            when(bloodRequestRepository.save(any())).thenReturn(request);
            when(hospitalRepository.findAllApprovedInState("Lagos"))
                    .thenReturn(List.of(sampleHospital, otherHospital));

            bloodBankService.createBloodRequest(request);

            // Should email otherHospital only (not the requesting hospital itself)
            verify(emailService, times(1)).sendBloodRequestAlert(
                    eq("admin@national.gov.ng"), any(), any(), any(), anyInt(), any());
        }

        @Test
        @DisplayName("Low stock alert triggered when units drop below 2")
        void lowStockAlert_triggeredBelow2Units() {
            BloodStock existingStock = BloodStock.builder()
                    .id(UUID.randomUUID())
                    .hospital(sampleHospital)
                    .bloodType(BloodType.O_NEGATIVE)
                    .unitsAvailable(3)
                    .build();

            when(hospitalRepository.findById(sampleHospital.getId()))
                    .thenReturn(Optional.of(sampleHospital));
            when(bloodStockRepository.findByHospitalIdAndBloodType(
                    sampleHospital.getId(), BloodType.O_NEGATIVE))
                    .thenReturn(Optional.of(existingStock));
            when(bloodStockRepository.save(any())).thenReturn(existingStock);

            bloodBankService.updateBloodStock(
                    sampleHospital.getId(), BloodType.O_NEGATIVE, 1, sampleUser);

            verify(emailService, times(1)).sendLowStockAlert(
                    eq(sampleHospital.getContactEmail()),
                    eq(sampleHospital.getName()),
                    eq(BloodType.O_NEGATIVE.getDisplay()),
                    eq(1));
        }

        @Test
        @DisplayName("No low stock alert when units stay at or above 2")
        void noLowStockAlert_whenUnitsAdequate() {
            BloodStock existingStock = BloodStock.builder()
                    .id(UUID.randomUUID())
                    .hospital(sampleHospital)
                    .bloodType(BloodType.A_POSITIVE)
                    .unitsAvailable(5)
                    .build();

            when(hospitalRepository.findById(sampleHospital.getId()))
                    .thenReturn(Optional.of(sampleHospital));
            when(bloodStockRepository.findByHospitalIdAndBloodType(any(), any()))
                    .thenReturn(Optional.of(existingStock));
            when(bloodStockRepository.save(any())).thenReturn(existingStock);

            bloodBankService.updateBloodStock(
                    sampleHospital.getId(), BloodType.A_POSITIVE, 4, sampleUser);

            verify(emailService, never()).sendLowStockAlert(any(), any(), any(), anyInt());
        }
    }

    // ─── DONOR REGISTRATION TESTS ─────────────────────────────────────

    @Nested
    @DisplayName("Donor Registration")
    class DonorRegistrationTests {

        @Test
        @DisplayName("Throws ConflictException when donor profile already exists")
        void throwsConflict_whenDonorAlreadyRegistered() {
            when(donorRepository.existsByUserId(sampleUser.getId())).thenReturn(true);

            Donor donor = Donor.builder()
                    .user(sampleUser)
                    .bloodType(BloodType.O_POSITIVE)
                    .dateOfBirth(LocalDate.now().minusYears(25))
                    .weightKg(70.0)
                    .build();

            assertThatThrownBy(() -> bloodBankService.registerDonor(donor))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("already exists");
        }
    }
}
