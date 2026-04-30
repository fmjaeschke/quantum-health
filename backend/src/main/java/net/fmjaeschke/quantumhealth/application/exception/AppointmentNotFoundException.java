package net.fmjaeschke.quantumhealth.application.exception;

import net.fmjaeschke.quantumhealth.domain.model.AppointmentId;

public class AppointmentNotFoundException extends RuntimeException {
    public AppointmentNotFoundException(AppointmentId id) {
        super("Appointment not found: " + id.value());
    }
}
