package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import net.fmjaeschke.quantumhealth.application.ports.out.EncounterRepository;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentId;
import net.fmjaeschke.quantumhealth.domain.model.Encounter;
import net.fmjaeschke.quantumhealth.domain.model.EncounterId;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class JpaEncounterRepositoryTest {

    @Inject
    EncounterRepository repository;

    @Test
    @Transactional
    void saveNew_persists_encounter_and_findById_returns_it() {
        var appointmentId = AppointmentId.generate();
        var doctorId = UserId.of("doctor-1");
        var patientId = PatientId.generate();

        var saved = repository.saveNew(Encounter.create(appointmentId, doctorId, patientId));

        assertThat(saved.getId()).isNotNull();
        var retrieved = repository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getAppointmentId()).isEqualTo(appointmentId);
        assertThat(retrieved.get().getDoctorId()).isEqualTo(doctorId);
        assertThat(retrieved.get().getPatientId()).isEqualTo(patientId);
        assertThat(retrieved.get().getCompletedAt()).isEmpty();
    }

    @Test
    @Transactional
    void findByAppointmentId_returns_linked_encounter() {
        var appointmentId = AppointmentId.generate();
        var saved = repository.saveNew(Encounter.create(appointmentId, UserId.of("doctor-1"), PatientId.generate()));

        var found = repository.findByAppointmentId(appointmentId);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    @Transactional
    void findById_returns_empty_when_not_found() {
        var found = repository.findById(EncounterId.generate());

        assertThat(found).isEmpty();
    }

    @Test
    @Transactional
    void save_persists_completedAt() {
        var saved = repository.saveNew(Encounter.create(AppointmentId.generate(), UserId.of("doctor-1"), PatientId.generate()));

        repository.save(saved.complete(Instant.now()));

        var retrieved = repository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getCompletedAt()).isPresent();
    }
}
