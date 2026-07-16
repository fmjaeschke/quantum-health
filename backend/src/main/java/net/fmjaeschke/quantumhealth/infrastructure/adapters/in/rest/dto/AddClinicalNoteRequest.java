package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddClinicalNoteRequest(@NotBlank @Size(max = 500) String content) {}
