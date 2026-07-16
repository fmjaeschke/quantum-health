package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import net.fmjaeschke.quantumhealth.domain.model.InsurancePolicy;
import net.fmjaeschke.quantumhealth.domain.model.InsuranceTier;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;

import java.util.UUID;

@Entity
@Table(name = "qh_insurance_policy")
public class JpaInsurancePolicy {

    @Id
    public UUID id;

    @Column(name = "patient_id", nullable = false)
    public UUID patientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public InsuranceTier tier;

    public InsurancePolicy toDomain() {
        return new InsurancePolicy(PatientId.of(patientId), tier);
    }
}
