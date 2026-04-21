package net.fmjaeschke.quantumhealth.application.exception;

import net.fmjaeschke.quantumhealth.domain.model.PatientId;

public class PatientNotFoundException extends RuntimeException {
    public PatientNotFoundException(PatientId id) {
        super("Patient not found: " + id.value());
    }
}
