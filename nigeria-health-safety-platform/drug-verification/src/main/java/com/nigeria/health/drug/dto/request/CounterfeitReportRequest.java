package com.nigeria.health.drug.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CounterfeitReportRequest {

    @NotBlank(message = "NAFDAC number is required")
    private String nafdacNumber;

    private String pharmacyName;
    private String sellerAddress;
    private String state;
    private String lga;

    @NotBlank(message = "Description is required")
    @Size(min = 20, message = "Please describe what makes you suspect this drug is counterfeit (min 20 characters)")
    private String description;

    private String evidencePhotoUrl;
}
