package net.fmjaeschke.quantumhealth.domain.model;

import net.fmjaeschke.quantumhealth.domain.exception.EncounterCompletedException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncounterTest {

    private static final AppointmentId APPOINTMENT = AppointmentId.generate();
    private static final UserId DOCTOR = UserId.of("dr-smith");
    private static final PatientId PATIENT = PatientId.generate();
    private static final Instant NOTE_AT = Instant.parse("2026-01-05T10:00:00Z");
    private static final Instant COMPLETED_AT = Instant.parse("2026-01-05T11:00:00Z");

    @Test
    void addNote_on_fresh_encounter_creates_version_1() {
        var encounter = Encounter.create(APPOINTMENT, DOCTOR, PATIENT);

        var updated = encounter.addNote("Patient presents with mild fever.", DOCTOR, NOTE_AT);

        assertThat(updated.getLatestNote()).isPresent();
        var note = updated.getLatestNote().orElseThrow();
        assertThat(note.version()).isEqualTo(1);
        assertThat(note.content()).isEqualTo("Patient presents with mild fever.");
        assertThat(note.authorId()).isEqualTo(DOCTOR);
        assertThat(note.createdAt()).isNotNull();
    }

    @Test
    void addNote_records_the_exact_instant_passed_in_as_createdAt() {
        var encounter = Encounter.create(APPOINTMENT, DOCTOR, PATIENT);

        var updated = encounter.addNote("Patient presents with mild fever.", DOCTOR, NOTE_AT);

        assertThat(updated.getLatestNote().orElseThrow().createdAt()).isEqualTo(NOTE_AT);
    }

    @Test
    void addNote_twice_creates_version_2_without_altering_version_1() {
        var encounter = Encounter.create(APPOINTMENT, DOCTOR, PATIENT)
                .addNote("First note.", DOCTOR, NOTE_AT);

        var updated = encounter.addNote("Second note.", DOCTOR, NOTE_AT);

        assertThat(updated.getNotes()).hasSize(2);
        assertThat(updated.getNotes().get(0).version()).isEqualTo(1);
        assertThat(updated.getNotes().get(0).content()).isEqualTo("First note.");
        assertThat(updated.getLatestNote()).isPresent();
        assertThat(updated.getLatestNote().orElseThrow().version()).isEqualTo(2);
        assertThat(updated.getLatestNote().orElseThrow().content()).isEqualTo("Second note.");
    }

    @Test
    void addNote_returns_new_instance_not_mutating_original() {
        var encounter = Encounter.create(APPOINTMENT, DOCTOR, PATIENT);

        encounter.addNote("A note.", DOCTOR, NOTE_AT);

        assertThat(encounter.getNotes()).isEmpty();
    }

    @Test
    void addNote_on_completed_encounter_throws() {
        var encounter = Encounter.reconstitute(EncounterId.generate(), APPOINTMENT, DOCTOR, PATIENT,
                Instant.now(), java.util.List.of());

        assertThatThrownBy(() -> encounter.addNote("Too late.", DOCTOR, NOTE_AT))
                .isInstanceOf(EncounterCompletedException.class);
    }

    @Test
    void complete_on_fresh_encounter_sets_completedAt() {
        var encounter = Encounter.create(APPOINTMENT, DOCTOR, PATIENT);

        var completed = encounter.complete(COMPLETED_AT);

        assertThat(completed.getCompletedAt()).isPresent();
    }

    @Test
    void complete_records_the_exact_instant_passed_in_as_completedAt() {
        var encounter = Encounter.create(APPOINTMENT, DOCTOR, PATIENT);

        var completed = encounter.complete(COMPLETED_AT);

        assertThat(completed.getCompletedAt()).contains(COMPLETED_AT);
    }

    @Test
    void complete_on_already_completed_encounter_throws() {
        var encounter = Encounter.reconstitute(EncounterId.generate(), APPOINTMENT, DOCTOR, PATIENT,
                Instant.now(), java.util.List.of());

        assertThatThrownBy(() -> encounter.complete(COMPLETED_AT))
                .isInstanceOf(EncounterCompletedException.class);
    }
}
