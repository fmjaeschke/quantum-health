package net.fmjaeschke.quantumhealth.domain.exception;

import net.fmjaeschke.quantumhealth.domain.model.PrescriptionStatus;

public class InvalidPrescriptionStateException extends RuntimeException {

    private final PrescriptionStatus current;
    private final String transition;

    public InvalidPrescriptionStateException(String transition, PrescriptionStatus current) {
        super("Cannot " + transition + " prescription in status: " + current);
        this.current = current;
        this.transition = transition;
    }

    public PrescriptionStatus getCurrent() { return current; }
    public String getTransition() { return transition; }
}
