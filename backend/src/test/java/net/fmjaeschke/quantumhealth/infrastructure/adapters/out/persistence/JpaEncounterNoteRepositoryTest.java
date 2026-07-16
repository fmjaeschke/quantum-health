package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import net.fmjaeschke.quantumhealth.application.ports.out.EncounterNoteRepository;
import net.fmjaeschke.quantumhealth.application.ports.out.EncounterRepository;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentId;
import net.fmjaeschke.quantumhealth.domain.model.Encounter;
import net.fmjaeschke.quantumhealth.domain.model.NoteVersion;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class JpaEncounterNoteRepositoryTest {

    @Inject
    EncounterRepository encounterRepository;

    @Inject
    EncounterNoteRepository noteRepository;

    @Test
    @Transactional
    void save_persists_note_and_findByEncounterId_returns_it_ordered_by_version() {
        var encounter = encounterRepository.saveNew(
                Encounter.create(AppointmentId.generate(), UserId.of("doctor-1"), PatientId.generate()));

        noteRepository.save(encounter.getId(), new NoteVersion(1, "First note.", UserId.of("doctor-1"), Instant.now()));
        noteRepository.save(encounter.getId(), new NoteVersion(2, "Second note.", UserId.of("doctor-1"), Instant.now()));

        var notes = noteRepository.findByEncounterId(encounter.getId());

        assertThat(notes).hasSize(2);
        assertThat(notes.get(0).version()).isEqualTo(1);
        assertThat(notes.get(0).content()).isEqualTo("First note.");
        assertThat(notes.get(1).version()).isEqualTo(2);
        assertThat(notes.get(1).content()).isEqualTo("Second note.");
    }

    @Test
    @Transactional
    void findByEncounterId_returns_empty_when_no_notes() {
        var encounter = encounterRepository.saveNew(
                Encounter.create(AppointmentId.generate(), UserId.of("doctor-1"), PatientId.generate()));

        var notes = noteRepository.findByEncounterId(encounter.getId());

        assertThat(notes).isEmpty();
    }
}
