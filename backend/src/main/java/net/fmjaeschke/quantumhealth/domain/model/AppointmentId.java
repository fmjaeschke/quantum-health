package net.fmjaeschke.quantumhealth.domain.model;

import java.util.UUID;

public record AppointmentId(UUID value) implements ResourceId {
    public static AppointmentId of(UUID value) {
        return new AppointmentId(value);
    }

    public static AppointmentId generate() {
        return new AppointmentId(UUID.randomUUID());
    }
}
