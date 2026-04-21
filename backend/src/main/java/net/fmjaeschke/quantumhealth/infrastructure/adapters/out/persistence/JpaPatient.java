package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import net.fmjaeschke.quantumhealth.domain.model.Patient;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "patient")
public class JpaPatient {

    @Id
    public UUID id;

    @Column(name = "first_name", nullable = false)
    public String firstName;

    @Column(name = "last_name", nullable = false)
    public String lastName;

    @Column(name = "date_of_birth", nullable = false)
    public LocalDate dateOfBirth;

    public static JpaPatient from(Patient patient) {
        var entity = new JpaPatient();
        entity.id = patient.getId()
                .value();
        entity.firstName = patient.getFirstName();
        entity.lastName = patient.getLastName();
        entity.dateOfBirth = patient.getDateOfBirth();
        return entity;
    }

    public Patient toDomain() {
        return Patient.reconstitute(PatientId.of(id), firstName, lastName, dateOfBirth);
    }
}
