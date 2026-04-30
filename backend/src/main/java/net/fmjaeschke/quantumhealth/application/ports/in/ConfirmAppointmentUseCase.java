package net.fmjaeschke.quantumhealth.application.ports.in;

import net.fmjaeschke.quantumhealth.domain.model.Appointment;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

public interface ConfirmAppointmentUseCase {
    Appointment confirm(AppointmentId id, UserId actor);
}
