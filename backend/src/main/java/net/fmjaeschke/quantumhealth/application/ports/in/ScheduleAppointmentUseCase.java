package net.fmjaeschke.quantumhealth.application.ports.in;

import net.fmjaeschke.quantumhealth.domain.model.Appointment;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

import java.time.LocalDateTime;

public interface ScheduleAppointmentUseCase {
    Appointment schedule(UserId actor, PatientId patientId, UserId doctorId,
                         LocalDateTime scheduledAt, String reason);
}
