package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.security;

import jakarta.enterprise.context.ApplicationScoped;
import net.fmjaeschke.quantumhealth.application.ports.out.AppointmentRepository;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

import java.util.Set;

/**
 * Phase 1 stub: no appointments exist yet. Replace with JpaAppointmentRepository in Phase 2.
 */
@ApplicationScoped
public class StubAppointmentRepository implements AppointmentRepository {

    @Override
    public boolean existsByDoctorAndPatient(UserId doctorId, PatientId patientId) {
        return false;
    }

    @Override
    public Set<PatientId> getPatientIdsByDoctor(UserId doctorId) {
        return Set.of();
    }
}
