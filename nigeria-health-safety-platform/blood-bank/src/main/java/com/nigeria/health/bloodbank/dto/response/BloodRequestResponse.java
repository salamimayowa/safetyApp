package com.nigeria.health.bloodbank.dto.response;

import com.nigeria.health.shared.enums.BloodType;
import com.nigeria.health.shared.enums.UrgencyLevel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class BloodRequestResponse {
    private UUID id;
    private UUID requestingHospitalId;
    private String requestingHospitalName;
    private String requestingHospitalState;
    private String requestingHospitalLga;
    private String requestingHospitalPhone;
    private BloodType bloodType;
    private String bloodTypeDisplay;
    private Integer unitsNeeded;
    private UrgencyLevel urgency;
    private String patientName;
    private String reason;
    private String status;
    private String fulfilledByHospitalName;
    private LocalDateTime createdAt;
    private LocalDateTime fulfilledAt;
    private LocalDateTime expiresAt;
}
