package net.fmjaeschke.quantumhealth.application.ports.out;

import net.fmjaeschke.quantumhealth.domain.model.Appointment;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentId;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface AppointmentRepository {
    boolean existsByDoctorAndPatient(UserId doctorId, PatientId patientId);
    Set<PatientId> getPatientIdsByDoctor(UserId doctorId);
    Appointment save(Appointment appointment);
    Optional<Appointment> findById(AppointmentId id);
    List<Appointment> findByDoctorId(UserId doctorId, UserId actor);
    List<Appointment> findAll(UserId actor);
}
