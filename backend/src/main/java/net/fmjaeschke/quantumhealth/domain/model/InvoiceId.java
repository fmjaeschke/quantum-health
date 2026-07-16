package net.fmjaeschke.quantumhealth.domain.model;

import java.util.UUID;

public record InvoiceId(UUID value) {
    public static InvoiceId of(UUID value) {
        return new InvoiceId(value);
    }

    public static InvoiceId generate() {
        return new InvoiceId(UUID.randomUUID());
    }
}
