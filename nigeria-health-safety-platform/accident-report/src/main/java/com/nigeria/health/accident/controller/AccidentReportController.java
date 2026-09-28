package com.nigeria.health.accident.controller;

import com.nigeria.health.accident.dto.request.AccidentReportRequest;
import com.nigeria.health.accident.dto.request.AccidentStatusUpdateRequest;
import com.nigeria.health.accident.dto.response.AccidentReportResponse;
import com.nigeria.health.accident.service.impl.AccidentReportServiceImpl;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Controller: AccidentReportController
 * Description: Submit accident reports, track status, manage FRSC responses.
 *              Report submission is PUBLIC — no login required.
 * Props: none
 */
@RestController
@RequestMapping("/api/v1/accidents")
@RequiredArgsConstructor
@Tag(name = "Accident Reports", description = "Road accident reporting and tracking")
public class AccidentReportController {

    private final AccidentReportServiceImpl accidentReportService;

    // ─── PUBLIC ENDPOINTS ─────────────────────────────────────────────

    /**
     * Submit a new accident report with optional photo uploads.
     * Any person (logged in or not) can submit a report.
     * Photos are sent as multipart/form-data.
     */
    @PostMapping(value = "/report", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Report a road accident (public — no login required)")
    public ResponseEntity<ApiResponse<AccidentReportResponse>> reportAccident(
            @Valid @RequestPart("data") AccidentReportRequest request,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos) {

        // In production, upload each photo to Cloudinary here and collect URLs.
        // For now we store placeholder URLs — swap with CloudinaryService.upload() calls.
        List<String> photoUrls = new ArrayList<>();
        if (photos != null) {
            photos.forEach(photo -> {
                // TODO: String url = cloudinaryService.upload(photo, "accident-photos");
                photoUrls.add("https://placeholder.cloudinary.com/" + photo.getOriginalFilename());
            });
        }

        AccidentReportResponse response = accidentReportService.submitReport(request, photoUrls);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Accident report submitted. Reference: " + response.getReferenceCode() +
                        ". Emergency services have been notified.", response));
    }

    /**
     * Track an accident report using its reference code.
     * Useful for reporter to check if FRSC has responded.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get accident report by ID (public)")
    public ResponseEntity<ApiResponse<AccidentReportResponse>> getReport(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Report found", accidentReportService.getById(id)));
    }

    @GetMapping("/track/{referenceCode}")
    @Operation(summary = "Track report by reference code (public)")
    public ResponseEntity<ApiResponse<AccidentReportResponse>> trackReport(
            @PathVariable String referenceCode) {
        return ResponseEntity.ok(ApiResponse.success(
                "Report found", accidentReportService.getByReferenceCode(referenceCode)));
    }

    // ─── FRSC OFFICER ENDPOINTS ───────────────────────────────────────

    @GetMapping
    @Operation(summary = "List accident reports in officer's state (FRSC_OFFICER)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('FRSC_OFFICER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<AccidentReportResponse>>> getReports(
            @RequestParam(required = false) String state,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        var result = state != null
                ? accidentReportService.getByState(state, pageable)
                : accidentReportService.getAllReports(pageable);

        return ResponseEntity.ok(ApiResponse.success("Reports retrieved",
                PagedResponse.from(result.map(accidentReportService::toResponse))));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update accident status (FRSC_OFFICER only)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('FRSC_OFFICER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AccidentReportResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AccidentStatusUpdateRequest request) {

        // In production, extract officer UUID from JWT token
        UUID officerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        AccidentReportResponse response =
                accidentReportService.updateStatus(id, request, officerId);
        return ResponseEntity.ok(ApiResponse.success("Report status updated", response));
    }

    // ─── ADMIN ENDPOINTS ──────────────────────────────────────────────

    @GetMapping("/admin/all")
    @Operation(summary = "List all reports nationwide (SUPER_ADMIN only)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<AccidentReportResponse>>> getAllReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success("All reports retrieved",
                PagedResponse.from(accidentReportService.getAllReports(pageable)
                        .map(accidentReportService::toResponse))));
    }
}
