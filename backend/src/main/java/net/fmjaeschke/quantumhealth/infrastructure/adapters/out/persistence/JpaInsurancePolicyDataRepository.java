package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaInsurancePolicyDataRepository {

    @Find
    Optional<JpaInsurancePolicy> findByPatientId(UUID patientId);
}
