package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.fmjaeschke.quantumhealth.application.ports.out.InsurancePolicyRepository;
import net.fmjaeschke.quantumhealth.domain.model.InsurancePolicy;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;

import java.util.Optional;

@ApplicationScoped
public class JpaInsurancePolicyRepository implements InsurancePolicyRepository {

    @Inject
    JpaInsurancePolicyDataRepository dataRepository;

    @Override
    public Optional<InsurancePolicy> findByPatientId(PatientId patientId) {
        return dataRepository.findByPatientId(patientId.value()).map(JpaInsurancePolicy::toDomain);
    }
}
