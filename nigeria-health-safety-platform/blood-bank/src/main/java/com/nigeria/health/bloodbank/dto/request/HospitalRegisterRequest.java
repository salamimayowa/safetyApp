package com.nigeria.health.bloodbank.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class HospitalRegisterRequest {

    @NotBlank(message = "Hospital name is required")
    @Size(max = 150)
    private String name;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "LGA is required")
    private String lga;

    private Double latitude;
    private Double longitude;

    @NotBlank(message = "Contact email is required")
    @Email
    private String contactEmail;

    @NotBlank(message = "Contact phone is required")
    private String contactPhone;
}
