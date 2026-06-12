package net.fmjaeschke.quantumhealth.domain.model;

import java.util.UUID;

public record PrescriptionId(UUID value) implements ResourceId {
    public static PrescriptionId of(UUID value) {
        return new PrescriptionId(value);
    }

    public static PrescriptionId generate() {
        return new PrescriptionId(UUID.randomUUID());
    }
}
