package net.fmjaeschke.quantumhealth.domain.model;

import java.util.UUID;

public record PatientId(UUID value) implements ResourceId {
    public static PatientId of(UUID value) {
        return new PatientId(value);
    }

    public static PatientId generate() {
        return new PatientId(UUID.randomUUID());
    }
}
