package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ScheduleAppointmentRequest(
        @NotNull UUID patientId,
        @NotBlank String doctorId,
        @NotNull @FutureOrPresent OffsetDateTime scheduledAt,
        @NotBlank String reason
) {}
