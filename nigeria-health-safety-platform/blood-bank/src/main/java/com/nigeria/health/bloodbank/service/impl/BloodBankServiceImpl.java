package com.nigeria.health.bloodbank.service.impl;

import com.nigeria.health.bloodbank.entity.*;
import com.nigeria.health.bloodbank.repository.*;
import com.nigeria.health.shared.email.EmailService;
import com.nigeria.health.shared.enums.BloodType;
import com.nigeria.health.shared.enums.UrgencyLevel;
import com.nigeria.health.shared.exception.BadRequestException;
import com.nigeria.health.shared.exception.ConflictException;
import com.nigeria.health.shared.exception.ResourceNotFoundException;
import com.nigeria.health.shared.sms.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service: BloodBankServiceImpl
 * Description: Core business logic for the Blood Bank Network module.
 *              Handles hospital registration, blood stock management,
 *              blood requests, donor management, and donation appointments.
 * Props: none (Spring-managed singleton)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BloodBankServiceImpl {

    private final HospitalRepository hospitalRepository;
    private final BloodStockRepository bloodStockRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final DonorRepository donorRepository;
    private final DonationRepository donationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SmsService smsService;

    // ─── HOSPITAL MANAGEMENT ────────────────────────────────────────

    /**
     * Register a new hospital. It starts as unapproved — SUPER_ADMIN must approve
     * before it appears in the public search.
     */
    @Transactional
    public Hospital registerHospital(Hospital hospital) {
        log.info("Registering new hospital: {}", hospital.getName());
        Hospital saved = hospitalRepository.save(hospital);
        log.info("Hospital registered with id: {} — pending admin approval", saved.getId());
        return saved;
    }

    /**
     * SUPER_ADMIN approves a hospital registration.
     * Sends approval email to the hospital admin.
     */
    @Transactional
    public Hospital approveHospital(UUID hospitalId, User approvedByAdmin) {
        Hospital hospital = findHospitalById(hospitalId);

        hospital.setIsApproved(true);
        hospital.setApprovedAt(LocalDateTime.now());
        hospital.setApprovedBy(approvedByAdmin);

        Hospital saved = hospitalRepository.save(hospital);
        log.info("Hospital approved: {} by admin: {}", hospital.getName(),
                approvedByAdmin.getEmail());

        // Initialise zero-stock records for all 8 blood types
        initialiseBloodStock(saved);

        // Email the hospital admin
        if (hospital.getAdminUser() != null) {
            emailService.sendHospitalApprovedEmail(
                    hospital.getContactEmail(),
                    hospital.getName(),
                    hospital.getAdminUser().getFullName()
            );
        }

        return saved;
    }

    /**
     * Get all approved hospitals (public endpoint — no auth needed).
     */
    public Page<Hospital> getApprovedHospitals(Pageable pageable) {
        return hospitalRepository.findByIsApprovedTrue(pageable);
    }

    /**
     * Get hospitals pending admin approval.
     */
    public Page<Hospital> getPendingHospitals(Pageable pageable) {
        return hospitalRepository.findByIsApprovedFalse(pageable);
    }

    // ─── BLOOD STOCK MANAGEMENT ─────────────────────────────────────

    /**
     * Update blood stock for a hospital.
     * Triggers a low-stock email alert if any type drops below 2 units.
     *
     * @param hospitalId  hospital being updated
     * @param bloodType   the blood type being updated
     * @param units       new unit count
     * @param updatedBy   the hospital admin performing the update
     */
    @Transactional
    public BloodStock updateBloodStock(UUID hospitalId, BloodType bloodType,
                                        int units, User updatedBy) {
        Hospital hospital = findHospitalById(hospitalId);

        BloodStock stock = bloodStockRepository
                .findByHospitalIdAndBloodType(hospitalId, bloodType)
                .orElseGet(() -> BloodStock.builder()
                        .hospital(hospital)
                        .bloodType(bloodType)
                        .unitsAvailable(0)
                        .build());

        int previousUnits = stock.getUnitsAvailable();
        stock.setUnitsAvailable(units);
        stock.setUpdatedBy(updatedBy);

        BloodStock saved = bloodStockRepository.save(stock);
        log.info("Blood stock updated — Hospital: {}, Type: {}, Units: {} → {}",
                hospital.getName(), bloodType, previousUnits, units);

        // Trigger low stock alert if units dropped below threshold
        if (units < 2 && previousUnits >= 2) {
            log.warn("LOW STOCK ALERT — Hospital: {}, Blood Type: {}, Units: {}",
                    hospital.getName(), bloodType.getDisplay(), units);
            emailService.sendLowStockAlert(
                    hospital.getContactEmail(),
                    hospital.getName(),
                    bloodType.getDisplay(),
                    units
            );
        }

        return saved;
    }

    /**
     * Get blood stock for a specific hospital.
     */
    public List<BloodStock> getHospitalStock(UUID hospitalId) {
        findHospitalById(hospitalId); // validate hospital exists
        return bloodStockRepository.findByHospitalId(hospitalId);
    }

    /**
     * Find hospitals with available blood of a specific type.
     * Returns LGA-level matches first, then state-level matches.
     * This is the proximity matching algorithm.
     */
    public List<Hospital> findHospitalsWithBlood(BloodType bloodType,
                                                   String state, String lga) {
        log.info("Searching for {} blood in {}, {}", bloodType.getDisplay(), lga, state);

        // Step 1: Search same LGA first (closest)
        List<Hospital> lgaResults =
                hospitalRepository.findHospitalsInLgaWithBlood(state, lga, bloodType);

        // Step 2: Search rest of state
        List<Hospital> stateResults =
                hospitalRepository.findHospitalsInStateWithBlood(state, bloodType);

        // Combine: LGA results first, then state results (without duplicates)
        List<Hospital> combined = new ArrayList<>(lgaResults);
        stateResults.stream()
                .filter(h -> lgaResults.stream().noneMatch(l -> l.getId().equals(h.getId())))
                .forEach(combined::add);

        log.info("Found {} hospitals with {} blood (LGA: {}, State total: {})",
                combined.size(), bloodType.getDisplay(), lgaResults.size(), stateResults.size());

        return combined;
    }

    // ─── BLOOD REQUESTS ─────────────────────────────────────────────

    /**
     * Create a blood request from a hospital.
     * CRITICAL requests immediately notify all hospitals in the state with matching blood.
     * Sets expiry to 24 hours from now.
     */
    @Transactional
    public BloodRequest createBloodRequest(BloodRequest request) {
        request.setStatus("OPEN");
        request.setExpiresAt(LocalDateTime.now().plusHours(24));

        BloodRequest saved = bloodRequestRepository.save(request);
        log.info("Blood request created — id: {}, type: {}, urgency: {}",
                saved.getId(), saved.getBloodType(), saved.getUrgency());

        // For CRITICAL requests, immediately notify all hospitals in the state
        if (UrgencyLevel.CRITICAL.equals(saved.getUrgency())) {
            notifyHospitalsOfCriticalRequest(saved);
        }

        return saved;
    }

    /**
     * A hospital fulfills an open blood request.
     */
    @Transactional
    public BloodRequest fulfillBloodRequest(UUID requestId, Hospital fulfillingHospital) {
        BloodRequest request = bloodRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Blood request not found: " + requestId));

        if (!request.isOpen()) {
            throw new BadRequestException(
                    "This blood request is no longer open. Status: " + request.getStatus());
        }

        request.setStatus("FULFILLED");
        request.setFulfilledByHospital(fulfillingHospital);
        request.setFulfilledAt(LocalDateTime.now());

        BloodRequest saved = bloodRequestRepository.save(request);
        log.info("Blood request {} fulfilled by hospital: {}",
                requestId, fulfillingHospital.getName());

        // Email the requesting hospital
        emailService.sendEmail(
                request.getRequestingHospital().getContactEmail(),
                "Blood Request Fulfilled ✅",
                "blood-request-fulfilled",
                java.util.Map.of(
                        "bloodType", request.getBloodType().getDisplay(),
                        "unitsNeeded", request.getUnitsNeeded(),
                        "fulfilledBy", fulfillingHospital.getName(),
                        "contactEmail", fulfillingHospital.getContactEmail(),
                        "contactPhone", fulfillingHospital.getContactPhone()
                )
        );

        return saved;
    }

    /**
     * Notify all hospitals in the same state about a CRITICAL blood request.
     * Skips the requesting hospital itself.
     */
    private void notifyHospitalsOfCriticalRequest(BloodRequest request) {
        String state = request.getRequestingHospital().getState();
        List<Hospital> hospitals =
                hospitalRepository.findAllApprovedInState(state);

        hospitals.stream()
                .filter(h -> !h.getId().equals(request.getRequestingHospital().getId()))
                .forEach(hospital -> emailService.sendBloodRequestAlert(
                        hospital.getContactEmail(),
                        request.getBloodType().getDisplay(),
                        request.getUrgency().name(),
                        request.getRequestingHospital().getName(),
                        request.getUnitsNeeded(),
                        request.getRequestingHospital().getContactEmail()
                ));

        log.info("CRITICAL blood request broadcast sent to {} hospitals in {}",
                hospitals.size() - 1, state);
    }

    // ─── DONOR MANAGEMENT ───────────────────────────────────────────

    /**
     * Register a new donor profile linked to an existing user.
     * One user can only have one donor profile.
     */
    @Transactional
    public Donor registerDonor(Donor donor) {
        if (donorRepository.existsByUserId(donor.getUser().getId())) {
            throw new ConflictException("A donor profile already exists for this account");
        }

        // Run initial eligibility check
        donor.checkEligibility();
        Donor saved = donorRepository.save(donor);
        log.info("Donor registered — userId: {}, bloodType: {}",
                donor.getUser().getId(), donor.getBloodType());
        return saved;
    }

    /**
     * Check and return current eligibility status for a donor.
     */
    @Transactional
    public Donor checkDonorEligibility(UUID userId) {
        Donor donor = donorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Donor profile not found for user: " + userId));
        donor.checkEligibility();
        return donorRepository.save(donor);
    }

    // ─── DONATION APPOINTMENTS ──────────────────────────────────────

    /**
     * Book a donation appointment at a hospital.
     * Validates donor eligibility before booking.
     */
    @Transactional
    public Donation bookDonationAppointment(UUID donorUserId, UUID hospitalId,
                                              LocalDateTime appointmentDate) {
        Donor donor = donorRepository.findByUserId(donorUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Donor profile not found"));

        if (!donor.checkEligibility()) {
            throw new BadRequestException(
                    "You are not eligible to donate: " + donor.getIneligibilityReason());
        }

        Hospital hospital = findHospitalById(hospitalId);

        Donation donation = Donation.builder()
                .donor(donor)
                .hospital(hospital)
                .appointmentDate(appointmentDate)
                .status("SCHEDULED")
                .build();

        Donation saved = donationRepository.save(donation);
        log.info("Donation appointment booked — donor: {}, hospital: {}, date: {}",
                donor.getUser().getEmail(), hospital.getName(), appointmentDate);

        // Send confirmation SMS and email
        smsService.sendDonationAppointmentConfirmation(
                donor.getUser().getPhone(),
                hospital.getName(),
                appointmentDate.toString()
        );

        emailService.sendEmail(
                donor.getUser().getEmail(),
                "Donation Appointment Confirmed 🩸",
                "donation-appointment",
                java.util.Map.of(
                        "donorName", donor.getUser().getFullName(),
                        "hospitalName", hospital.getName(),
                        "hospitalAddress", hospital.getAddress(),
                        "appointmentDate", appointmentDate.toString()
                )
        );

        return saved;
    }

    /**
     * Mark a donation as completed.
     * Updates the donor's last donation date and increments total donations.
     * Increases the hospital's blood stock.
     */
    @Transactional
    public Donation completeDonation(UUID donationId) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Donation not found: " + donationId));

        donation.setStatus("COMPLETED");
        donation.setDonationDate(LocalDateTime.now());

        // Update donor record
        Donor donor = donation.getDonor();
        donor.setLastDonationDate(java.time.LocalDate.now());
        donor.setTotalDonations(donor.getTotalDonations() + 1);
        donor.setIsEligible(false); // not eligible again until 56 days pass
        donor.setIneligibilityReason("Must wait 56 days before donating again");
        donorRepository.save(donor);

        // Increase hospital blood stock
        updateBloodStock(
                donation.getHospital().getId(),
                donor.getBloodType(),
                getHospitalStock(donation.getHospital().getId()).stream()
                        .filter(s -> s.getBloodType().equals(donor.getBloodType()))
                        .mapToInt(BloodStock::getUnitsAvailable)
                        .findFirst()
                        .orElse(0) + 1,
                null
        );

        Donation saved = donationRepository.save(donation);
        log.info("Donation completed — donor: {}, hospital: {}",
                donor.getUser().getEmail(), donation.getHospital().getName());

        return saved;
    }

    // ─── Helper methods ─────────────────────────────────────────────

    public Hospital findHospitalById(UUID id) {
        return hospitalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Hospital not found with id: " + id));
    }

    /**
     * Create zero-stock records for all 8 blood types when a hospital is first approved.
     */
    private void initialiseBloodStock(Hospital hospital) {
        for (BloodType bt : BloodType.values()) {
            boolean alreadyExists = bloodStockRepository
                    .findByHospitalIdAndBloodType(hospital.getId(), bt)
                    .isPresent();
            if (!alreadyExists) {
                bloodStockRepository.save(BloodStock.builder()
                        .hospital(hospital)
                        .bloodType(bt)
                        .unitsAvailable(0)
                        .build());
            }
        }
        log.info("Blood stock records initialised for hospital: {}", hospital.getName());
    }
}
