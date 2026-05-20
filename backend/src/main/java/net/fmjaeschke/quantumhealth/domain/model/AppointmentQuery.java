package net.fmjaeschke.quantumhealth.domain.model;

import java.util.Optional;

public record AppointmentQuery(
        Optional<AppointmentStatus> statusFilter,
        Optional<UserId> doctorIdFilter,
        int page,
        int pageSize) {

    public static AppointmentQuery unfiltered(int page, int pageSize) {
        return new AppointmentQuery(Optional.empty(), Optional.empty(), page, pageSize);
    }
}
