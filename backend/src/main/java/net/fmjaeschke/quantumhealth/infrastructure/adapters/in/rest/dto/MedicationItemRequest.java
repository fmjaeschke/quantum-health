package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record MedicationItemRequest(
        @NotBlank String drugName,
        @NotBlank String dosage,
        @NotBlank String frequency
) {}
