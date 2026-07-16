package net.fmjaeschke.quantumhealth.application.exception;

import net.fmjaeschke.quantumhealth.domain.model.InvoiceId;

public class InvoiceNotFoundException extends RuntimeException {
    public InvoiceNotFoundException(InvoiceId id) {
        super("Invoice not found: " + id.value());
    }
}
