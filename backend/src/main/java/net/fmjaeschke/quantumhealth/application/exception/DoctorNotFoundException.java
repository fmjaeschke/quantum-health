package net.fmjaeschke.quantumhealth.application.exception;

import net.fmjaeschke.quantumhealth.domain.model.UserId;

public class DoctorNotFoundException extends RuntimeException {
    public DoctorNotFoundException(UserId id) {
        super("Doctor not found: " + id.value());
    }
}
