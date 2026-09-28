package com.nigeria.health.bloodbank.controller;

import com.nigeria.health.bloodbank.dto.request.BloodStockUpdateRequest;
import com.nigeria.health.bloodbank.dto.request.HospitalRegisterRequest;
import com.nigeria.health.bloodbank.dto.response.BloodStockResponse;
import com.nigeria.health.bloodbank.dto.response.HospitalResponse;
import com.nigeria.health.bloodbank.entity.Hospital;
import com.nigeria.health.bloodbank.entity.User;
import com.nigeria.health.bloodbank.service.impl.BloodBankServiceImpl;
import com.nigeria.health.shared.enums.BloodType;
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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller: HospitalController
 * Description: Hospital registration, approval, blood stock management, and blood search.
 * Props: none
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Hospitals & Blood Stock", description = "Manage hospitals and blood inventory")
public class HospitalController {

    private final BloodBankServiceImpl bloodBankService;

    // ─── PUBLIC ENDPOINTS ────────────────────────────────────────────

    @GetMapping("/hospitals")
    @Operation(summary = "List all approved hospitals (public)")
    public ResponseEntity<ApiResponse<PagedResponse<HospitalResponse>>> getHospitals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        var hospitalPage = bloodBankService.getApprovedHospitals(pageable);
        var response = PagedResponse.from(hospitalPage.map(this::toResponse));
        return ResponseEntity.ok(ApiResponse.success("Hospitals retrieved", response));
    }

    @GetMapping("/hospitals/{id}")
    @Operation(summary = "Get hospital details (public)")
    public ResponseEntity<ApiResponse<HospitalResponse>> getHospital(
            @PathVariable UUID id) {
        Hospital hospital = bloodBankService.findHospitalById(id);
        return ResponseEntity.ok(ApiResponse.success("Hospital found", toResponse(hospital)));
    }

    @GetMapping("/blood-stock/search")
    @Operation(summary = "Find hospitals with a specific blood type (public)")
    public ResponseEntity<ApiResponse<List<HospitalResponse>>> searchBlood(
            @RequestParam BloodType bloodType,
            @RequestParam String state,
            @RequestParam String lga) {

        List<HospitalResponse> results = bloodBankService
                .findHospitalsWithBlood(bloodType, state, lga)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(
                results.size() + " hospital(s) found with " +
                bloodType.getDisplay() + " blood in " + state, results));
    }

    // ─── HOSPITAL SELF-REGISTRATION ──────────────────────────────────

    @PostMapping("/hospitals/register")
    @Operation(summary = "Hospital self-registers (requires HOSPITAL_ADMIN role)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    public ResponseEntity<ApiResponse<HospitalResponse>> registerHospital(
            @Valid @RequestBody HospitalRegisterRequest request,
            @AuthenticationPrincipal User currentUser) {

        Hospital hospital = Hospital.builder()
                .name(request.getName())
                .address(request.getAddress())
                .state(request.getState())
                .lga(request.getLga())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .adminUser(currentUser)
                .build();

        Hospital saved = bloodBankService.registerHospital(hospital);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Hospital registration submitted. Awaiting admin approval.",
                        toResponse(saved)));
    }

    // ─── BLOOD STOCK MANAGEMENT ──────────────────────────────────────

    @GetMapping("/hospitals/{id}/stock")
    @Operation(summary = "View blood stock for a hospital")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<List<BloodStockResponse>>> getStock(
            @PathVariable UUID id) {
        List<BloodStockResponse> stock = bloodBankService.getHospitalStock(id)
                .stream()
                .map(s -> BloodStockResponse.builder()
                        .id(s.getId())
                        .bloodType(s.getBloodType())
                        .bloodTypeDisplay(s.getBloodType().getDisplay())
                        .unitsAvailable(s.getUnitsAvailable())
                        .isLow(s.isLow())
                        .lastUpdated(s.getLastUpdated())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Blood stock retrieved", stock));
    }

    @PutMapping("/hospitals/{id}/stock")
    @Operation(summary = "Update blood stock (HOSPITAL_ADMIN only)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('HOSPITAL_ADMIN')")
    public ResponseEntity<ApiResponse<BloodStockResponse>> updateStock(
            @PathVariable UUID id,
            @Valid @RequestBody BloodStockUpdateRequest request,
            @AuthenticationPrincipal User currentUser) {

        var updated = bloodBankService.updateBloodStock(
                id, request.getBloodType(), request.getUnits(), currentUser);

        BloodStockResponse response = BloodStockResponse.builder()
                .id(updated.getId())
                .bloodType(updated.getBloodType())
                .bloodTypeDisplay(updated.getBloodType().getDisplay())
                .unitsAvailable(updated.getUnitsAvailable())
                .isLow(updated.isLow())
                .lastUpdated(updated.getLastUpdated())
                .build();

        return ResponseEntity.ok(ApiResponse.success("Blood stock updated", response));
    }

    // ─── ADMIN ENDPOINTS ─────────────────────────────────────────────

    @GetMapping("/admin/hospitals/pending")
    @Operation(summary = "List hospitals pending approval (SUPER_ADMIN only)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<HospitalResponse>>> getPendingHospitals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        var result = PagedResponse.from(
                bloodBankService.getPendingHospitals(pageable).map(this::toResponse));
        return ResponseEntity.ok(ApiResponse.success("Pending hospitals retrieved", result));
    }

    @PutMapping("/admin/hospitals/{id}/approve")
    @Operation(summary = "Approve a hospital registration (SUPER_ADMIN only)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<HospitalResponse>> approveHospital(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {

        Hospital approved = bloodBankService.approveHospital(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(
                "Hospital approved successfully", toResponse(approved)));
    }

    // ─── Mapper ──────────────────────────────────────────────────────

    private HospitalResponse toResponse(Hospital h) {
        return HospitalResponse.builder()
                .id(h.getId())
                .name(h.getName())
                .address(h.getAddress())
                .state(h.getState())
                .lga(h.getLga())
                .latitude(h.getLatitude())
                .longitude(h.getLongitude())
                .contactEmail(h.getContactEmail())
                .contactPhone(h.getContactPhone())
                .isApproved(h.getIsApproved())
                .approvedAt(h.getApprovedAt())
                .createdAt(h.getCreatedAt())
                .build();
    }
}
