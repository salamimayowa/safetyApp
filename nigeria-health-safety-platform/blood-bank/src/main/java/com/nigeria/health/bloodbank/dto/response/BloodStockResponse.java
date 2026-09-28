package com.nigeria.health.bloodbank.dto.response;

import com.nigeria.health.shared.enums.BloodType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class BloodStockResponse {
    private UUID id;
    private BloodType bloodType;
    private String bloodTypeDisplay;
    private Integer unitsAvailable;
    private Boolean isLow;
    private LocalDateTime lastUpdated;
}
