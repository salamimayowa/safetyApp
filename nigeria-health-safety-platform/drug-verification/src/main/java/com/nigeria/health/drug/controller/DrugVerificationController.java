package com.nigeria.health.drug.controller;

import com.nigeria.health.drug.dto.request.CounterfeitReportRequest;
import com.nigeria.health.drug.dto.request.DrugRegisterRequest;
import com.nigeria.health.drug.dto.response.DrugResponse;
import com.nigeria.health.drug.dto.response.DrugVerificationResponse;
import com.nigeria.health.drug.entity.CounterfeitReport;
import com.nigeria.health.drug.entity.Drug;
import com.nigeria.health.drug.service.impl.DrugVerificationServiceImpl;
import com.nigeria.health.drug.service.impl.DrugVerificationServiceImpl.DrugVerificationResult;
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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Controller: DrugVerificationController
 * Description: Drug verification, registration, approval, and counterfeit reporting.
 *              /api/v1/drugs/verify/** is PUBLIC — anyone can verify a drug.
 * Props: none
 */
@RestController
@RequestMapping("/api/v1/drugs")
@RequiredArgsConstructor
@Tag(name = "Drug Verification", description = "NAFDAC drug authenticity verification")
public class DrugVerificationController {

    private final DrugVerificationServiceImpl drugService;

    // ─── PUBLIC ENDPOINTS ─────────────────────────────────────────────

    /**
     * Verify a drug by its NAFDAC number.
     * PUBLIC — no login required. Any Nigerian can scan and verify.
     */
    @GetMapping("/verify/{nafdacNumber}")
    @Operation(summary = "Verify a drug by NAFDAC number (public — no login required)")
    public ResponseEntity<ApiResponse<DrugVerificationResponse>> verifyDrug(
            @PathVariable String nafdacNumber,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String lga,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {

        DrugVerificationResult result = drugService.verify(
                nafdacNumber, state, lga, latitude, longitude, null);

        DrugVerificationResponse response = buildVerificationResponse(nafdacNumber, result);

        String httpMessage = switch (result.result()) {
            case "GENUINE"     -> "✅ Drug is GENUINE and approved by NAFDAC";
            case "COUNTERFEIT" -> "🚫 WARNING: This drug has been flagged as COUNTERFEIT";
            case "EXPIRED"     -> "⚠️ This drug has EXPIRED — do not use";
            case "NOT_FOUND"   -> "❓ Drug not found in the NAFDAC registry";
            default            -> "Verification complete";
        };

        return ResponseEntity.ok(ApiResponse.success(httpMessage, response));
    }

    /**
     * Report a suspected counterfeit drug.
     * PUBLIC — no login required to protect reporter privacy.
     */
    @PostMapping("/report-counterfeit")
    @Operation(summary = "Report a counterfeit or suspicious drug (public)")
    public ResponseEntity<ApiResponse<Void>> reportCounterfeit(
            @Valid @RequestBody CounterfeitReportRequest request) {

        CounterfeitReport report = CounterfeitReport.builder()
                .nafdacNumber(request.getNafdacNumber().trim().toUpperCase())
                .pharmacyName(request.getPharmacyName())
                .sellerAddress(request.getSellerAddress())
                .state(request.getState())
                .lga(request.getLga())
                .description(request.getDescription())
                .evidencePhotoUrl(request.getEvidencePhotoUrl())
                .status("PENDING")
                .build();

        drugService.reportCounterfeit(report);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Thank you for your report. Our team will investigate this drug. " +
                        "Your report helps protect Nigerians from counterfeit medicines."));
    }

    // ─── PHARMACY_ADMIN ENDPOINTS ─────────────────────────────────────

    @PostMapping
    @Operation(summary = "Register a drug for NAFDAC verification (PHARMACY_ADMIN)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('PHARMACY_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DrugResponse>> registerDrug(
            @Valid @RequestBody DrugRegisterRequest request) {

        Drug drug = Drug.builder()
                .brandName(request.getBrandName())
                .genericName(request.getGenericName())
                .nafdacNumber(request.getNafdacNumber().trim().toUpperCase())
                .manufacturer(request.getManufacturer())
                .countryOfOrigin(request.getCountryOfOrigin())
                .category(request.getCategory())
                .registrationDate(request.getRegistrationDate())
                .expiryDate(request.getExpiryDate())
                .dosageForm(request.getDosageForm())
                .storageInstructions(request.getStorageInstructions())
                .build();

        Drug saved = drugService.registerDrug(drug);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Drug submitted for review. It will appear in verification results " +
                        "once approved by the admin.", toResponse(saved)));
    }

    @GetMapping("/my-submissions")
    @Operation(summary = "Get drugs I submitted (PHARMACY_ADMIN)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('PHARMACY_ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<DrugResponse>>> getMySubmissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // TODO: extract real userId from JWT
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success("Your submissions",
                PagedResponse.from(drugService.getMySubmissions(userId, pageable)
                        .map(this::toResponse))));
    }

    // ─── ADMIN ENDPOINTS ──────────────────────────────────────────────

    @GetMapping("/admin/pending")
    @Operation(summary = "List drugs awaiting approval (SUPER_ADMIN)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<DrugResponse>>> getPendingDrugs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success("Pending drugs",
                PagedResponse.from(drugService.getPendingDrugs(pageable).map(this::toResponse))));
    }

    @PutMapping("/admin/{id}/approve")
    @Operation(summary = "Approve a drug registration (SUPER_ADMIN)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DrugResponse>> approveDrug(@PathVariable UUID id) {
        UUID adminId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Drug approved = drugService.approveDrug(id, adminId);
        return ResponseEntity.ok(ApiResponse.success("Drug approved successfully",
                toResponse(approved)));
    }

    @DeleteMapping("/admin/{id}/reject")
    @Operation(summary = "Reject a drug registration (SUPER_ADMIN)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> rejectDrug(@PathVariable UUID id) {
        drugService.rejectDrug(id);
        return ResponseEntity.ok(ApiResponse.success("Drug registration rejected and removed"));
    }

    @GetMapping
    @Operation(summary = "Browse approved drugs")
    public ResponseEntity<ApiResponse<PagedResponse<DrugResponse>>> getDrugs(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var pageable = PageRequest.of(page, size, Sort.by("brandName").ascending());
        var result = search != null
                ? drugService.searchDrugs(search, pageable)
                : drugService.getApprovedDrugs(pageable);

        return ResponseEntity.ok(ApiResponse.success("Drugs retrieved",
                PagedResponse.from(result.map(this::toResponse))));
    }

    // ─── MAPPERS ──────────────────────────────────────────────────────

    private DrugVerificationResponse buildVerificationResponse(
            String nafdacNumber, DrugVerificationResult result) {

        Drug d = result.drug();
        return DrugVerificationResponse.builder()
                .nafdacNumber(nafdacNumber.toUpperCase())
                .result(result.result())
                .resultMessage(buildResultMessage(result.result()))
                .brandName(d != null ? d.getBrandName() : null)
                .genericName(d != null ? d.getGenericName() : null)
                .manufacturer(d != null ? d.getManufacturer() : null)
                .category(d != null ? d.getCategory() : null)
                .dosageForm(d != null ? d.getDosageForm() : null)
                .expiryDate(d != null ? d.getExpiryDate() : null)
                .storageInstructions(d != null ? d.getStorageInstructions() : null)
                .verifiedAt(LocalDateTime.now())
                .build();
    }

    private String buildResultMessage(String result) {
        return switch (result) {
            case "GENUINE"     -> "This drug is registered and approved by NAFDAC. It is safe to use.";
            case "COUNTERFEIT" -> "DANGER: This NAFDAC number is associated with a counterfeit product. Do NOT use this drug and report the seller immediately.";
            case "EXPIRED"     -> "This drug's registration or product expiry date has passed. Do not use expired medication.";
            case "NOT_FOUND"   -> "This NAFDAC number is not in our registry. The drug may be unregistered or counterfeit. Exercise caution and report if suspicious.";
            default            -> "Verification complete.";
        };
    }

    private DrugResponse toResponse(Drug d) {
        return DrugResponse.builder()
                .id(d.getId())
                .brandName(d.getBrandName())
                .genericName(d.getGenericName())
                .nafdacNumber(d.getNafdacNumber())
                .manufacturer(d.getManufacturer())
                .countryOfOrigin(d.getCountryOfOrigin())
                .category(d.getCategory())
                .registrationDate(d.getRegistrationDate())
                .expiryDate(d.getExpiryDate())
                .dosageForm(d.getDosageForm())
                .storageInstructions(d.getStorageInstructions())
                .isApproved(d.getIsApproved())
                .isExpired(d.isExpired())
                .approvedAt(d.getApprovedAt())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
