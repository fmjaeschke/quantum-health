package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RegisterPatientRequest(@NotBlank String firstName, @NotBlank String lastName,
                                     @NotNull LocalDate dateOfBirth) {
}
