package net.fmjaeschke.quantumhealth.domain.exception;

import net.fmjaeschke.quantumhealth.domain.model.InvoiceStatus;

public class InvalidInvoiceStateException extends RuntimeException {

    private final InvoiceStatus current;
    private final String transition;

    public InvalidInvoiceStateException(String transition, InvoiceStatus current) {
        super("Cannot " + transition + " invoice in status: " + current);
        this.current = current;
        this.transition = transition;
    }

    public InvoiceStatus getCurrent() { return current; }
    public String getTransition() { return transition; }
}
