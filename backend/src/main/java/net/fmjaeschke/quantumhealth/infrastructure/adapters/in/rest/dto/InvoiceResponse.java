package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto;

import net.fmjaeschke.quantumhealth.domain.model.InvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        UUID encounterId,
        UUID patientId,
        BigDecimal totalAmount,
        BigDecimal insurerAmount,
        BigDecimal patientCopay,
        InvoiceStatus status,
        Instant patientPaidAt
) {}
