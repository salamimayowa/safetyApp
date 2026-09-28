package com.nigeria.health.bloodbank.dto.request;

import com.nigeria.health.shared.enums.BloodType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DonorRegisterRequest {

    @NotNull(message = "Blood type is required")
    private BloodType bloodType;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotNull(message = "Weight is required")
    @DecimalMin(value = "50.0", message = "Minimum weight for donation is 50kg")
    private Double weightKg;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "LGA is required")
    private String lga;
}
