package com.nigeria.health.drug.dto.response;

import com.nigeria.health.shared.enums.DrugCategory;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class DrugResponse {
    private UUID id;
    private String brandName;
    private String genericName;
    private String nafdacNumber;
    private String manufacturer;
    private String countryOfOrigin;
    private DrugCategory category;
    private LocalDate registrationDate;
    private LocalDate expiryDate;
    private String dosageForm;
    private String storageInstructions;
    private Boolean isApproved;
    private Boolean isExpired;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
}
