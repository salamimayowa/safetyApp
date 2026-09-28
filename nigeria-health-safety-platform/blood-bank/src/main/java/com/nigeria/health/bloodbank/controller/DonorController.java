package com.nigeria.health.bloodbank.controller;

import com.nigeria.health.bloodbank.dto.request.BookAppointmentRequest;
import com.nigeria.health.bloodbank.dto.request.DonorRegisterRequest;
import com.nigeria.health.bloodbank.dto.response.DonationResponse;
import com.nigeria.health.bloodbank.dto.response.DonorResponse;
import com.nigeria.health.bloodbank.entity.Donor;
import com.nigeria.health.bloodbank.entity.User;
import com.nigeria.health.bloodbank.repository.DonationRepository;
import com.nigeria.health.bloodbank.repository.DonorRepository;
import com.nigeria.health.bloodbank.repository.UserRepository;
import com.nigeria.health.bloodbank.service.impl.BloodBankServiceImpl;
import com.nigeria.health.shared.exception.ResourceNotFoundException;
import com.nigeria.health.shared.response.ApiResponse;
import com.nigeria.health.shared.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controller: DonorController
 * Description: Donor registration, eligibility checks, and appointment booking.
 * Props: none
 */
@RestController
@RequestMapping("/api/v1/donors")
@RequiredArgsConstructor
@Tag(name = "Donors", description = "Donor registration and appointment management")
@SecurityRequirement(name = "bearerAuth")
public class DonorController {

    private final BloodBankServiceImpl bloodBankService;
    private final DonorRepository donorRepository;
    private final DonationRepository donationRepository;
    private final UserRepository userRepository;

    @PostMapping("/register")
    @Operation(summary = "Register as a blood donor")
    @PreAuthorize("hasRole('DONOR')")
    public ResponseEntity<ApiResponse<DonorResponse>> register(
            @Valid @RequestBody DonorRegisterRequest request,
            @AuthenticationPrincipal User currentUser) {

        Donor donor = Donor.builder()
                .user(currentUser)
                .bloodType(request.getBloodType())
                .dateOfBirth(request.getDateOfBirth())
                .weightKg(request.getWeightKg())
                .state(request.getState())
                .lga(request.getLga())
                .build();

        Donor saved = bloodBankService.registerDonor(donor);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Donor profile created successfully", toResponse(saved)));
    }

    @GetMapping("/me")
    @Operation(summary = "Get my donor profile")
    @PreAuthorize("hasRole('DONOR')")
    public ResponseEntity<ApiResponse<DonorResponse>> getMyProfile(
            @AuthenticationPrincipal User currentUser) {
        Donor donor = donorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Donor profile not found. Please register as a donor first."));
        return ResponseEntity.ok(ApiResponse.success("Donor profile retrieved", toResponse(donor)));
    }

    @GetMapping("/eligibility")
    @Operation(summary = "Check if I am eligible to donate today")
    @PreAuthorize("hasRole('DONOR')")
    public ResponseEntity<ApiResponse<DonorResponse>> checkEligibility(
            @AuthenticationPrincipal User currentUser) {
        Donor donor = bloodBankService.checkDonorEligibility(currentUser.getId());
        String message = donor.getIsEligible()
                ? "You are eligible to donate blood! ✅"
                : "You are not eligible: " + donor.getIneligibilityReason();
        return ResponseEntity.ok(ApiResponse.success(message, toResponse(donor)));
    }

    @PostMapping("/book-appointment")
    @Operation(summary = "Book a blood donation appointment")
    @PreAuthorize("hasRole('DONOR')")
    public ResponseEntity<ApiResponse<DonationResponse>> bookAppointment(
            @Valid @RequestBody BookAppointmentRequest request,
            @AuthenticationPrincipal User currentUser) {

        var donation = bloodBankService.bookDonationAppointment(
                currentUser.getId(),
                request.getHospitalId(),
                request.getAppointmentDate()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Appointment booked! Check your email for details.",
                        toDonationResponse(donation)));
    }

    @GetMapping("/appointments")
    @Operation(summary = "View my donation appointments")
    @PreAuthorize("hasRole('DONOR')")
    public ResponseEntity<ApiResponse<PagedResponse<DonationResponse>>> getAppointments(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Donor donor = donorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Donor profile not found"));

        var pageable = PageRequest.of(page, size, Sort.by("appointmentDate").descending());
        var result = PagedResponse.from(
                donationRepository.findByDonorId(donor.getId(), pageable)
                        .map(this::toDonationResponse));

        return ResponseEntity.ok(ApiResponse.success("Appointments retrieved", result));
    }

    // ─── Mappers ─────────────────────────────────────────────────────

    private DonorResponse toResponse(Donor d) {
        return DonorResponse.builder()
                .id(d.getId())
                .userId(d.getUser().getId())
                .fullName(d.getUser().getFullName())
                .email(d.getUser().getEmail())
                .phone(d.getUser().getPhone())
                .bloodType(d.getBloodType())
                .bloodTypeDisplay(d.getBloodType().getDisplay())
                .dateOfBirth(d.getDateOfBirth())
                .age(d.getAge())
                .weightKg(d.getWeightKg())
                .state(d.getState())
                .lga(d.getLga())
                .lastDonationDate(d.getLastDonationDate())
                .totalDonations(d.getTotalDonations())
                .isEligible(d.getIsEligible())
                .ineligibilityReason(d.getIneligibilityReason())
                .createdAt(d.getCreatedAt())
                .build();
    }

    private DonationResponse toDonationResponse(
            com.nigeria.health.bloodbank.entity.Donation d) {
        return DonationResponse.builder()
                .id(d.getId())
                .donorId(d.getDonor().getId())
                .donorName(d.getDonor().getUser().getFullName())
                .hospitalId(d.getHospital().getId())
                .hospitalName(d.getHospital().getName())
                .hospitalAddress(d.getHospital().getAddress())
                .appointmentDate(d.getAppointmentDate())
                .donationDate(d.getDonationDate())
                .status(d.getStatus())
                .notes(d.getNotes())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
