package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record ScheduleAppointmentRequest(
        @NotNull UUID patientId,
        @NotBlank String patientName,
        @NotBlank String doctorId,
        @NotBlank String doctorName,
        @NotNull @Future LocalDateTime scheduledAt
) {}
