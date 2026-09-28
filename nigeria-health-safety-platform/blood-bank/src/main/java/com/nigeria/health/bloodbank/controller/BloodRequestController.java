package com.nigeria.health.bloodbank.controller;

import com.nigeria.health.bloodbank.dto.request.BloodRequestCreateRequest;
import com.nigeria.health.bloodbank.dto.response.BloodRequestResponse;
import com.nigeria.health.bloodbank.entity.BloodRequest;
import com.nigeria.health.bloodbank.entity.Hospital;
import com.nigeria.health.bloodbank.entity.User;
import com.nigeria.health.bloodbank.repository.BloodRequestRepository;
import com.nigeria.health.bloodbank.repository.HospitalRepository;
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

import java.util.UUID;

/**
 * Controller: BloodRequestController
 * Description: Create, view, and fulfil blood requests between hospitals.
 * Props: none
 */
@RestController
@RequestMapping("/api/v1/blood-requests")
@RequiredArgsConstructor
@Tag(name = "Blood Requests", description = "Hospital blood request management")
@SecurityRequirement(name = "bearerAuth")
public class BloodRequestController {

    private final BloodBankServiceImpl bloodBankService;
    private final BloodRequestRepository bloodRequestRepository;
    private final HospitalRepository hospitalRepository;

    @PostMapping
    @Operation(summary = "Create a blood request (HOSPITAL_ADMIN only)")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    public ResponseEntity<ApiResponse<BloodRequestResponse>> createRequest(
            @Valid @RequestBody BloodRequestCreateRequest req,
            @AuthenticationPrincipal User currentUser) {

        Hospital hospital = hospitalRepository.findByAdminUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hospital found for your account. Please register your hospital first."));

        BloodRequest bloodRequest = BloodRequest.builder()
                .requestingHospital(hospital)
                .bloodType(req.getBloodType())
                .unitsNeeded(req.getUnitsNeeded())
                .urgency(req.getUrgency())
                .patientName(req.getPatientName())
                .reason(req.getReason())
                .build();

        BloodRequest saved = bloodBankService.createBloodRequest(bloodRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Blood request created successfully", toResponse(saved)));
    }

    @GetMapping
    @Operation(summary = "List all open blood requests")
    public ResponseEntity<ApiResponse<PagedResponse<BloodRequestResponse>>> getRequests(
            @RequestParam(defaultValue = "OPEN") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        var result = PagedResponse.from(
                bloodRequestRepository.findByStatus(status, pageable).map(this::toResponse));
        return ResponseEntity.ok(ApiResponse.success("Blood requests retrieved", result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a specific blood request")
    public ResponseEntity<ApiResponse<BloodRequestResponse>> getRequest(
            @PathVariable UUID id) {
        BloodRequest request = bloodRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Blood request not found: " + id));
        return ResponseEntity.ok(ApiResponse.success("Blood request found", toResponse(request)));
    }

    @PutMapping("/{id}/fulfill")
    @Operation(summary = "Fulfill a blood request (HOSPITAL_ADMIN only)")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    public ResponseEntity<ApiResponse<BloodRequestResponse>> fulfillRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {

        Hospital hospital = hospitalRepository.findByAdminUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hospital found for your account"));

        BloodRequest fulfilled = bloodBankService.fulfillBloodRequest(id, hospital);
        return ResponseEntity.ok(ApiResponse.success("Blood request fulfilled", toResponse(fulfilled)));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel a blood request (requesting hospital only)")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> cancelRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {

        BloodRequest request = bloodRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found: " + id));

        request.setStatus("CANCELLED");
        bloodRequestRepository.save(request);
        return ResponseEntity.ok(ApiResponse.success("Blood request cancelled"));
    }

    // ─── Mapper ──────────────────────────────────────────────────────

    private BloodRequestResponse toResponse(BloodRequest r) {
        return BloodRequestResponse.builder()
                .id(r.getId())
                .requestingHospitalId(r.getRequestingHospital().getId())
                .requestingHospitalName(r.getRequestingHospital().getName())
                .requestingHospitalState(r.getRequestingHospital().getState())
                .requestingHospitalLga(r.getRequestingHospital().getLga())
                .requestingHospitalPhone(r.getRequestingHospital().getContactPhone())
                .bloodType(r.getBloodType())
                .bloodTypeDisplay(r.getBloodType().getDisplay())
                .unitsNeeded(r.getUnitsNeeded())
                .urgency(r.getUrgency())
                .patientName(r.getPatientName())
                .reason(r.getReason())
                .status(r.getStatus())
                .fulfilledByHospitalName(r.getFulfilledByHospital() != null
                        ? r.getFulfilledByHospital().getName() : null)
                .createdAt(r.getCreatedAt())
                .fulfilledAt(r.getFulfilledAt())
                .expiresAt(r.getExpiresAt())
                .build();
    }
}
