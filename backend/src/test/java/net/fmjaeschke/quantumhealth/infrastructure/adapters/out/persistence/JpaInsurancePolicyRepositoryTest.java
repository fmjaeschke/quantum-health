package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import net.fmjaeschke.quantumhealth.application.ports.out.InsurancePolicyRepository;
import net.fmjaeschke.quantumhealth.domain.model.InsuranceTier;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class JpaInsurancePolicyRepositoryTest {

    @Inject
    InsurancePolicyRepository repository;

    @Inject
    EntityManager entityManager;

    private PatientId persistPolicy(InsuranceTier tier) {
        var patientId = PatientId.generate();

        var entity = new JpaInsurancePolicy();
        entity.id = UUID.randomUUID();
        entity.patientId = patientId.value();
        entity.tier = tier;
        entityManager.persist(entity);
        entityManager.flush();

        return patientId;
    }

    @Test
    @Transactional
    void findByPatientId_returns_policy_when_found() {
        var patientId = persistPolicy(InsuranceTier.GOLD);

        var found = repository.findByPatientId(patientId);

        assertThat(found).isPresent();
        assertThat(found.get().patientId()).isEqualTo(patientId);
        assertThat(found.get().tier()).isEqualTo(InsuranceTier.GOLD);
    }

    @Test
    @Transactional
    void findByPatientId_returns_empty_when_not_found() {
        var found = repository.findByPatientId(PatientId.generate());

        assertThat(found).isEmpty();
    }
}
