package com.nigeria.health.bloodbank.dto.request;

import com.nigeria.health.shared.enums.BloodType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BloodStockUpdateRequest {

    @NotNull(message = "Blood type is required")
    private BloodType bloodType;

    @NotNull(message = "Units is required")
    @Min(value = 0, message = "Units cannot be negative")
    private Integer units;
}
