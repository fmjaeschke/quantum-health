package net.fmjaeschke.quantumhealth.application.ports.out;

import net.fmjaeschke.quantumhealth.domain.model.Appointment;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentId;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentPage;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentQuery;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

import java.util.Optional;
import java.util.Set;

public interface AppointmentRepository {
    boolean existsByDoctorAndPatient(UserId doctorId, PatientId patientId);
    Set<PatientId> getPatientIdsByDoctor(UserId doctorId);
    Appointment save(Appointment appointment);
    /**
     * Persists a new appointment.
     *
     * <p><strong>Contract:</strong> implementations MUST enforce that no active appointment
     * already exists for the same doctor–patient pair. If such a booking exists, the
     * implementation must throw {@link net.fmjaeschke.quantumhealth.application.exception.DuplicateAppointmentException}.
     * The application layer relies on this guarantee and does not perform a pre-check
     * (which would introduce a TOCTOU race under concurrent requests).
     */
    Appointment saveNew(Appointment appointment);
    Optional<Appointment> findById(AppointmentId id);
    AppointmentPage findAll(AppointmentQuery query);
}
