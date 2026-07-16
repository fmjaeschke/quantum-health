package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto;

import java.time.Instant;
import java.util.UUID;

public record EncounterResponse(
        UUID id,
        UUID appointmentId,
        String doctorId,
        UUID patientId,
        Instant completedAt,
        String latestNoteContent
) {}
