package com.nigeria.health.accident.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO: AccidentStatusUpdateRequest
 * Description: Used by FRSC officers to update the status of an accident report
 *              and add notes/updates visible to the reporter.
 */
@Data
public class AccidentStatusUpdateRequest {

    @NotBlank(message = "New status is required")
    private String newStatus; // ACKNOWLEDGED, RESPONDERS_DISPATCHED, RESOLVED

    @NotBlank(message = "Update message is required")
    private String message;
}
