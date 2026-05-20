package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto;

import net.fmjaeschke.quantumhealth.domain.model.AppointmentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        UUID patientId,
        String patientName,
        String doctorId,
        String doctorName,
        LocalDateTime scheduledAt,
        String reason,
        AppointmentStatus status
) {}
