package com.nigeria.health.accident.dto.response;

import com.nigeria.health.shared.enums.AccidentSeverity;
import com.nigeria.health.shared.enums.BloodType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO: AccidentReportResponse
 * Description: Returned after creating or fetching an accident report.
 *              Includes current status, FRSC station notified, and all updates.
 */
@Data
@Builder
public class AccidentReportResponse {
    private UUID id;
    private String referenceCode;
    private Double latitude;
    private Double longitude;
    private String state;
    private String lga;
    private String landmark;
    private AccidentSeverity severity;
    private Integer numberOfCasualties;
    private Integer numberOfVehicles;
    private String description;
    private BloodType bloodTypeNeeded;
    private String status;
    private String nearestFrscStationName;
    private String nearestFrscStationPhone;
    private List<String> photoUrls;
    private List<AccidentUpdateResponse> updates;
    private LocalDateTime createdAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
}
