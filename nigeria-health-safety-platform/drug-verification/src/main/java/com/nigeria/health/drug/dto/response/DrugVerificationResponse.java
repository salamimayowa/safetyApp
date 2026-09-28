package com.nigeria.health.drug.dto.response;

import com.nigeria.health.shared.enums.DrugCategory;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class DrugVerificationResponse {
    private String nafdacNumber;
    /** GENUINE, COUNTERFEIT, NOT_FOUND, EXPIRED */
    private String result;
    private String resultMessage;
    private String brandName;
    private String genericName;
    private String manufacturer;
    private DrugCategory category;
    private String dosageForm;
    private LocalDate expiryDate;
    private String storageInstructions;
    private LocalDateTime verifiedAt;
}
