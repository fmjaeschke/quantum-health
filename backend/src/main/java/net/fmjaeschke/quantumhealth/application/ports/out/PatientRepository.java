package net.fmjaeschke.quantumhealth.application.ports.out;

import net.fmjaeschke.quantumhealth.domain.model.Patient;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.PatientPage;
import net.fmjaeschke.quantumhealth.domain.model.PatientQuery;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

import java.util.Optional;

public interface PatientRepository {
    PatientId save(Patient patient);

    Optional<Patient> findById(PatientId id);

    PatientPage findAll(PatientQuery query);

    PatientPage findByDoctor(UserId doctorId, PatientQuery query);
}
