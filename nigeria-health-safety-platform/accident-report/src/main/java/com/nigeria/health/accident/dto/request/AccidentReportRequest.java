package com.nigeria.health.accident.dto.request;

import com.nigeria.health.shared.enums.AccidentSeverity;
import com.nigeria.health.shared.enums.BloodType;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request DTO: AccidentReportRequest
 * Description: Payload submitted by a citizen reporting a road accident.
 *              Photos are uploaded as separate multipart files.
 */
@Data
public class AccidentReportRequest {

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
    private Double longitude;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "LGA is required")
    private String lga;

    private String landmark;

    @NotNull(message = "Severity is required")
    private AccidentSeverity severity;

    @NotNull(message = "Number of casualties is required")
    @Min(value = 0) @Max(value = 500)
    private Integer numberOfCasualties;

    @Min(value = 1) @Max(value = 100)
    private Integer numberOfVehicles = 1;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    /** Optional — reporter's phone for SMS status updates */
    private String reporterPhone;

    /** Optional — if reporter knows what blood type is needed */
    private BloodType bloodTypeNeeded;
}
