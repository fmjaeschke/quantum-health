package net.fmjaeschke.quantumhealth.domain.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum AppointmentStatus {
    PENDING, CONFIRMED, ARRIVED, IN_PROGRESS, COMPLETED, CANCELLED;

    // Statuses considered "active" — must match the uq_active_appointment partial index predicate (migration 0005)
    public static final Set<AppointmentStatus> ACTIVE_STATUSES =
            Collections.unmodifiableSet(EnumSet.of(PENDING, CONFIRMED, ARRIVED, IN_PROGRESS));
}
