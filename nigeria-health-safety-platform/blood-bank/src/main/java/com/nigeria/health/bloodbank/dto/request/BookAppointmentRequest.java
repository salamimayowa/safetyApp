package com.nigeria.health.bloodbank.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class BookAppointmentRequest {

    @NotNull(message = "Hospital ID is required")
    private UUID hospitalId;

    @NotNull(message = "Appointment date is required")
    @Future(message = "Appointment date must be in the future")
    private LocalDateTime appointmentDate;
}
