package com.nigeria.health.accident.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AccidentUpdateResponse {
    private UUID id;
    private String message;
    private String statusChangedTo;
    private LocalDateTime createdAt;
}
