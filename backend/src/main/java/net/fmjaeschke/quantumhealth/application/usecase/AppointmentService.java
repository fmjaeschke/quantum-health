package net.fmjaeschke.quantumhealth.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import net.fmjaeschke.quantumhealth.application.exception.AppointmentNotFoundException;
import net.fmjaeschke.quantumhealth.application.ports.in.CancelAppointmentUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ConfirmAppointmentUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ListAppointmentsUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ReadAppointmentUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ScheduleAppointmentUseCase;
import net.fmjaeschke.quantumhealth.application.ports.out.AppointmentRepository;
import net.fmjaeschke.quantumhealth.domain.model.Appointment;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentId;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
@Transactional
public class AppointmentService implements
        ScheduleAppointmentUseCase, ReadAppointmentUseCase, ListAppointmentsUseCase,
        ConfirmAppointmentUseCase, CancelAppointmentUseCase {

    private final AppointmentRepository repository;

    public AppointmentService(AppointmentRepository repository) {
        this.repository = repository;
    }

    @Override
    public Appointment schedule(UserId actor, PatientId patientId, String patientName,
                                UserId doctorId, String doctorName, LocalDateTime scheduledAt) {
        return repository.save(
                Appointment.schedule(patientId, patientName, doctorId, doctorName, scheduledAt));
    }

    @Override
    public Appointment findById(AppointmentId id, UserId actor) {
        return repository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException(id));
    }

    @Override
    public List<Appointment> listByDoctor(UserId doctorId, UserId actor) {
        return repository.findByDoctorId(doctorId, actor);
    }

    @Override
    public List<Appointment> findAll(UserId actor) {
        return repository.findAll(actor);
    }

    @Override
    public Appointment confirm(AppointmentId id, UserId actor) {
        var appointment = repository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException(id));
        return repository.save(appointment.confirm());
    }

    @Override
    public Appointment cancel(AppointmentId id, UserId actor) {
        var appointment = repository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException(id));
        return repository.save(appointment.cancel());
    }
}
