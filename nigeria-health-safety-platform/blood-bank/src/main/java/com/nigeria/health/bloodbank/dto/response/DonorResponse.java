package com.nigeria.health.bloodbank.dto.response;

import com.nigeria.health.shared.enums.BloodType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class DonorResponse {
    private UUID id;
    private UUID userId;
    private String fullName;
    private String email;
    private String phone;
    private BloodType bloodType;
    private String bloodTypeDisplay;
    private LocalDate dateOfBirth;
    private Integer age;
    private Double weightKg;
    private String state;
    private String lga;
    private LocalDate lastDonationDate;
    private Integer totalDonations;
    private Boolean isEligible;
    private String ineligibilityReason;
    private LocalDateTime createdAt;
}
