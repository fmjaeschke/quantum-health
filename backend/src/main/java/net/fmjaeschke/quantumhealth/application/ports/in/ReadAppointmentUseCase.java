package net.fmjaeschke.quantumhealth.application.ports.in;

import net.fmjaeschke.quantumhealth.domain.model.Appointment;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

public interface ReadAppointmentUseCase {
    Appointment findById(AppointmentId id, UserId actor);
}
