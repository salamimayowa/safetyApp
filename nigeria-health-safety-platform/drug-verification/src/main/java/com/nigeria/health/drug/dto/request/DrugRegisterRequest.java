package com.nigeria.health.drug.dto.request;

import com.nigeria.health.shared.enums.DrugCategory;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DrugRegisterRequest {

    @NotBlank(message = "Brand name is required")
    @Size(max = 150)
    private String brandName;

    @NotBlank(message = "Generic name is required")
    @Size(max = 150)
    private String genericName;

    @NotBlank(message = "NAFDAC number is required")
    @Pattern(regexp = "^[A-Z0-9\\-]{5,20}$",
             message = "NAFDAC number must be 5-20 alphanumeric characters")
    private String nafdacNumber;

    @NotBlank(message = "Manufacturer is required")
    private String manufacturer;

    private String countryOfOrigin;
    private DrugCategory category;
    private LocalDate registrationDate;

    @Future(message = "Expiry date must be in the future")
    private LocalDate expiryDate;

    private String dosageForm;
    private String storageInstructions;
}
