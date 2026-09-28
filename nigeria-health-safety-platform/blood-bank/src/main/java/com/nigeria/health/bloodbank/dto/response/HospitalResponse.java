package com.nigeria.health.bloodbank.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class HospitalResponse {
    private UUID id;
    private String name;
    private String address;
    private String state;
    private String lga;
    private Double latitude;
    private Double longitude;
    private String contactEmail;
    private String contactPhone;
    private Boolean isApproved;
    private LocalDateTime approvedAt;
    private List<BloodStockResponse> bloodStock;
    private LocalDateTime createdAt;
}
