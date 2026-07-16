package net.fmjaeschke.quantumhealth.domain.model;

import java.util.UUID;

public record EncounterId(UUID value) implements ResourceId {
    public static EncounterId of(UUID value) {
        return new EncounterId(value);
    }

    public static EncounterId generate() {
        return new EncounterId(UUID.randomUUID());
    }
}
