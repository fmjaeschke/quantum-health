package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record ScheduleAppointmentRequest(
        @NotNull UUID patientId,
        @NotBlank String doctorId,
        @NotNull @FutureOrPresent LocalDateTime scheduledAt,
        @NotBlank String reason
) {}
