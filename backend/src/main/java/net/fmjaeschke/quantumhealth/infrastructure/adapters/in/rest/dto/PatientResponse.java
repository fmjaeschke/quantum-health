package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto;

import java.time.LocalDate;
import java.util.UUID;

public record PatientResponse(UUID id, String firstName, String lastName, LocalDate dateOfBirth) {
}
