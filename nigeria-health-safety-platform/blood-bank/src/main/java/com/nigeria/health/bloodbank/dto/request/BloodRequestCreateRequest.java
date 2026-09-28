package com.nigeria.health.bloodbank.dto.request;

import com.nigeria.health.shared.enums.BloodType;
import com.nigeria.health.shared.enums.UrgencyLevel;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class BloodRequestCreateRequest {

    @NotNull(message = "Blood type is required")
    private BloodType bloodType;

    @NotNull(message = "Units needed is required")
    @Min(value = 1, message = "Must request at least 1 unit")
    @Max(value = 50, message = "Cannot request more than 50 units at once")
    private Integer unitsNeeded;

    @NotNull(message = "Urgency level is required")
    private UrgencyLevel urgency;

    private String patientName;

    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;
}
