package net.fmjaeschke.quantumhealth.application.ports.out;

import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

import java.util.Set;

public interface AppointmentRepository {
    /**
     * Returns true if the doctor has at least one appointment with the patient.
     */
    boolean existsByDoctorAndPatient(UserId doctorId, PatientId patientId);

    /**
     * Returns all patient IDs for which the doctor has at least one appointment.
     */
    Set<PatientId> getPatientIdsByDoctor(UserId doctorId);
}
