package net.fmjaeschke.quantumhealth.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import net.fmjaeschke.quantumhealth.application.exception.AppointmentNotFoundException;
import net.fmjaeschke.quantumhealth.application.exception.DoctorNotFoundException;
import net.fmjaeschke.quantumhealth.application.exception.PatientNotFoundException;
import net.fmjaeschke.quantumhealth.application.ports.in.CancelAppointmentUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.CheckInUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ConfirmAppointmentUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.FindDoctorsUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ListAppointmentsUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ReadAppointmentUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ScheduleAppointmentUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.StartEncounterUseCase;
import net.fmjaeschke.quantumhealth.application.ports.out.AccessPolicy;
import net.fmjaeschke.quantumhealth.application.ports.out.AppointmentRepository;
import net.fmjaeschke.quantumhealth.application.ports.out.DoctorPort;
import net.fmjaeschke.quantumhealth.application.ports.out.PatientRepository;
import net.fmjaeschke.quantumhealth.domain.model.Appointment;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentId;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentPage;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentQuery;
import net.fmjaeschke.quantumhealth.domain.model.Doctor;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Transactional
public class AppointmentService implements
        ScheduleAppointmentUseCase, ReadAppointmentUseCase, ListAppointmentsUseCase,
        ConfirmAppointmentUseCase, CancelAppointmentUseCase, CheckInUseCase, StartEncounterUseCase,
        FindDoctorsUseCase {

    private final AppointmentRepository repository;
    private final PatientRepository patientRepository;
    private final DoctorPort doctorPort;
    private final AccessPolicy accessPolicy;

    public AppointmentService(AppointmentRepository repository,
                              PatientRepository patientRepository,
                              DoctorPort doctorPort,
                              AccessPolicy accessPolicy) {
        this.repository = repository;
        this.patientRepository = patientRepository;
        this.doctorPort = doctorPort;
        this.accessPolicy = accessPolicy;
    }

    @Override
    public Appointment schedule(UserId actor, PatientId patientId, UserId doctorId,
                                LocalDateTime scheduledAt, String reason) {
        var patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException(patientId));
        var doctor = doctorPort.findById(doctorId)
                .orElseThrow(() -> new DoctorNotFoundException(doctorId));
        var patientName = patient.getFirstName() + " " + patient.getLastName();
        return repository.save(
                Appointment.schedule(patientId, patientName, doctorId, doctor.displayName(), scheduledAt, reason));
    }

    @Override
    public List<Doctor> findDoctors() {
        return doctorPort.findByRole("DOCTOR");
    }

    @Override
    public Appointment findById(AppointmentId id, UserId actor) {
        return repository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException(id));
    }

    @Override
    public AppointmentPage list(AppointmentQuery query, UserId actor) {
        AppointmentQuery effective = accessPolicy.isDoctor(actor)
                ? new AppointmentQuery(query.statusFilter(), Optional.of(actor), query.page(), query.pageSize())
                : query;
        return repository.findAll(effective);
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

    @Override
    public Appointment checkIn(AppointmentId id, UserId actor) {
        var appointment = repository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException(id));
        return repository.save(appointment.checkIn());
    }

    @Override
    public Appointment start(AppointmentId id, UserId actor) {
        var appointment = repository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException(id));
        return repository.save(appointment.start());
    }
}
