package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto;

import net.fmjaeschke.quantumhealth.domain.model.MedicationItem;
import net.fmjaeschke.quantumhealth.domain.model.PrescriptionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PrescriptionResponse(
        UUID id,
        UUID patientId,
        String patientName,
        String doctorId,
        String doctorName,
        List<MedicationItem> medications,
        PrescriptionStatus status,
        Instant issuedAt,
        Instant fulfilledAt,
        String fulfilledBy,
        Instant cancelledAt,
        String cancelledBy,
        String cancelledReason,
        Instant expiredAt
) {}
