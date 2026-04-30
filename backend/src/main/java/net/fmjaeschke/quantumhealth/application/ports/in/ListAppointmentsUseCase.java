package net.fmjaeschke.quantumhealth.application.ports.in;

import net.fmjaeschke.quantumhealth.domain.model.Appointment;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

import java.util.List;

public interface ListAppointmentsUseCase {
    List<Appointment> listByDoctor(UserId doctorId, UserId actor);
    List<Appointment> findAll(UserId actor);
}
