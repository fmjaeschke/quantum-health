package net.fmjaeschke.quantumhealth.domain.exception;

import net.fmjaeschke.quantumhealth.domain.model.AppointmentStatus;

public class InvalidAppointmentStateException extends RuntimeException {

    private final AppointmentStatus current;
    private final String transition;

    public InvalidAppointmentStateException(String transition, AppointmentStatus current) {
        super("Cannot " + transition + " appointment in status: " + current);
        this.current = current;
        this.transition = transition;
    }

    public AppointmentStatus getCurrent() { return current; }
    public String getTransition() { return transition; }
}
