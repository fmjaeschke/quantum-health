package net.fmjaeschke.quantumhealth.application.ports.out;

import net.fmjaeschke.quantumhealth.domain.model.InsurancePolicy;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;

import java.util.Optional;

public interface InsurancePolicyRepository {
    Optional<InsurancePolicy> findByPatientId(PatientId patientId);
}
