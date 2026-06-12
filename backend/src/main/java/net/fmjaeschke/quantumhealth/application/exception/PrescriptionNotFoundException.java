package net.fmjaeschke.quantumhealth.application.exception;

import net.fmjaeschke.quantumhealth.domain.model.PrescriptionId;

public class PrescriptionNotFoundException extends RuntimeException {
    public PrescriptionNotFoundException(PrescriptionId id) {
        super("Prescription not found: " + id.value());
    }
}
