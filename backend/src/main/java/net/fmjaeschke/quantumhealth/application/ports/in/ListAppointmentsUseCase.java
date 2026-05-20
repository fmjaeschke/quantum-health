package net.fmjaeschke.quantumhealth.application.ports.in;

import net.fmjaeschke.quantumhealth.domain.model.AppointmentPage;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentQuery;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

public interface ListAppointmentsUseCase {
    AppointmentPage list(AppointmentQuery query, UserId actor);
}
