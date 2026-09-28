package com.nigeria.health.bloodbank.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class DonationResponse {
    private UUID id;
    private UUID donorId;
    private String donorName;
    private UUID hospitalId;
    private String hospitalName;
    private String hospitalAddress;
    private LocalDateTime appointmentDate;
    private LocalDateTime donationDate;
    private String status;
    private String notes;
    private LocalDateTime createdAt;
}
