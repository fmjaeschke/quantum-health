package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record IssuePrescriptionRequest(
        @NotNull UUID patientId,
        @NotNull @NotEmpty @Valid List<MedicationItemRequest> medications
) {}
