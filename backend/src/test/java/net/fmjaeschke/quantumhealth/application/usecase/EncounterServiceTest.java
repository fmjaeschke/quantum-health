package net.fmjaeschke.quantumhealth.application.usecase;

import net.fmjaeschke.quantumhealth.application.exception.AccessDeniedException;
import net.fmjaeschke.quantumhealth.application.exception.AppointmentNotFoundException;
import net.fmjaeschke.quantumhealth.application.exception.EncounterNotFoundException;
import net.fmjaeschke.quantumhealth.application.ports.out.AccessPolicy;
import net.fmjaeschke.quantumhealth.application.ports.out.AppointmentRepository;
import net.fmjaeschke.quantumhealth.application.ports.out.DomainEventPublisher;
import net.fmjaeschke.quantumhealth.application.ports.out.EncounterNoteRepository;
import net.fmjaeschke.quantumhealth.application.ports.out.EncounterRepository;
import net.fmjaeschke.quantumhealth.domain.exception.EncounterCompletedException;
import net.fmjaeschke.quantumhealth.domain.model.Appointment;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentId;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentPage;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentQuery;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentStatus;
import net.fmjaeschke.quantumhealth.domain.model.Encounter;
import net.fmjaeschke.quantumhealth.domain.model.EncounterCompletedEvent;
import net.fmjaeschke.quantumhealth.domain.model.EncounterId;
import net.fmjaeschke.quantumhealth.domain.model.NoteVersion;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.Permission;
import net.fmjaeschke.quantumhealth.domain.model.ResourceId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncounterServiceTest {

    static final UserId ASSIGNED_DOCTOR = UserId.of("dr-smith");
    static final UserId OTHER_TREATING_DOCTOR = UserId.of("dr-jones");
    static final UserId UNRELATED_DOCTOR = UserId.of("dr-other");
    static final PatientId PATIENT = PatientId.generate();
    static final AppointmentId APPOINTMENT_ID = AppointmentId.generate();
    static final Instant FIXED_NOW = Instant.parse("2026-01-15T12:00:00Z");
    static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    static Encounter encounter() {
        return Encounter.create(APPOINTMENT_ID, ASSIGNED_DOCTOR, PATIENT);
    }

    @Test
    void findById_returns_encounter_for_assigned_doctor() {
        var encounter = encounter();
        var repo = new FakeEncounterRepo(encounter);
        var service = new EncounterService(repo, new FakeAccessPolicy(Set.of(ASSIGNED_DOCTOR)), new FakeEncounterNoteRepository(),
                new FakeAppointmentRepo(), new RecordingDomainEventPublisher(), FIXED_CLOCK);

        var found = service.findById(encounter.getId(), ASSIGNED_DOCTOR);

        assertThat(found.getId()).isEqualTo(encounter.getId());
    }

    @Test
    void findById_returns_encounter_for_doctor_who_has_treated_patient_elsewhere() {
        var encounter = encounter();
        var repo = new FakeEncounterRepo(encounter);
        var service = new EncounterService(repo, new FakeAccessPolicy(Set.of(OTHER_TREATING_DOCTOR)), new FakeEncounterNoteRepository(),
                new FakeAppointmentRepo(), new RecordingDomainEventPublisher(), FIXED_CLOCK);

        var found = service.findById(encounter.getId(), OTHER_TREATING_DOCTOR);

        assertThat(found.getId()).isEqualTo(encounter.getId());
    }

    @Test
    void findById_throws_EncounterNotFoundException_for_unrelated_doctor() {
        var encounter = encounter();
        var repo = new FakeEncounterRepo(encounter);
        var service = new EncounterService(repo, new FakeAccessPolicy(Set.of(ASSIGNED_DOCTOR)), new FakeEncounterNoteRepository(),
                new FakeAppointmentRepo(), new RecordingDomainEventPublisher(), FIXED_CLOCK);

        assertThatThrownBy(() -> service.findById(encounter.getId(), UNRELATED_DOCTOR))
                .isInstanceOf(EncounterNotFoundException.class);
    }

    @Test
    void findById_throws_when_not_found() {
        var service = new EncounterService(new FakeEncounterRepo(), new FakeAccessPolicy(Set.of()), new FakeEncounterNoteRepository(),
                new FakeAppointmentRepo(), new RecordingDomainEventPublisher(), FIXED_CLOCK);

        var id = EncounterId.generate();
        assertThatThrownBy(() -> service.findById(id, ASSIGNED_DOCTOR))
                .isInstanceOf(EncounterNotFoundException.class);
    }

    @Test
    void addNote_by_assigned_doctor_creates_version_1() {
        var encounter = encounter();
        var repo = new FakeEncounterRepo(encounter);
        var notes = new FakeEncounterNoteRepository();
        var service = new EncounterService(repo, new FakeAccessPolicy(Set.of()), notes,
                new FakeAppointmentRepo(), new RecordingDomainEventPublisher(), FIXED_CLOCK);

        var updated = service.addNote(encounter.getId(), "First note.", ASSIGNED_DOCTOR);

        assertThat(updated.getLatestNote()).isPresent();
        assertThat(updated.getLatestNote().orElseThrow().version()).isEqualTo(1);
        assertThat(notes.findByEncounterId(encounter.getId())).hasSize(1);
    }

    @Test
    void addNote_twice_creates_version_2_without_altering_version_1() {
        var encounter = encounter();
        var repo = new FakeEncounterRepo(encounter);
        var notes = new FakeEncounterNoteRepository();
        var service = new EncounterService(repo, new FakeAccessPolicy(Set.of()), notes,
                new FakeAppointmentRepo(), new RecordingDomainEventPublisher(), FIXED_CLOCK);

        service.addNote(encounter.getId(), "First note.", ASSIGNED_DOCTOR);
        var updated = service.addNote(encounter.getId(), "Second note.", ASSIGNED_DOCTOR);

        assertThat(updated.getLatestNote().orElseThrow().version()).isEqualTo(2);
        var stored = notes.findByEncounterId(encounter.getId());
        assertThat(stored).hasSize(2);
        assertThat(stored.get(0).content()).isEqualTo("First note.");
        assertThat(stored.get(1).content()).isEqualTo("Second note.");
    }

    @Test
    void addNote_records_the_clocks_current_instant_as_createdAt() {
        var encounter = encounter();
        var repo = new FakeEncounterRepo(encounter);
        var notes = new FakeEncounterNoteRepository();
        var service = new EncounterService(repo, new FakeAccessPolicy(Set.of()), notes,
                new FakeAppointmentRepo(), new RecordingDomainEventPublisher(), FIXED_CLOCK);

        var updated = service.addNote(encounter.getId(), "First note.", ASSIGNED_DOCTOR);

        assertThat(updated.getLatestNote().orElseThrow().createdAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    void addNote_by_non_assigned_doctor_throws_AccessDeniedException() {
        var encounter = encounter();
        var repo = new FakeEncounterRepo(encounter);
        var service = new EncounterService(repo, new FakeAccessPolicy(Set.of(), true), new FakeEncounterNoteRepository(),
                new FakeAppointmentRepo(), new RecordingDomainEventPublisher(), FIXED_CLOCK);

        assertThatThrownBy(() -> service.addNote(encounter.getId(), "Note.", UNRELATED_DOCTOR))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void addNote_on_completed_encounter_throws_EncounterCompletedException_even_though_access_check_passed() {
        var completed = Encounter.reconstitute(EncounterId.generate(), APPOINTMENT_ID, ASSIGNED_DOCTOR, PATIENT,
                Instant.now(), List.of());
        var repo = new FakeEncounterRepo(completed);
        var service = new EncounterService(repo, new FakeAccessPolicy(Set.of()), new FakeEncounterNoteRepository(),
                new FakeAppointmentRepo(), new RecordingDomainEventPublisher(), FIXED_CLOCK);

        assertThatThrownBy(() -> service.addNote(completed.getId(), "Too late.", ASSIGNED_DOCTOR))
                .isInstanceOf(EncounterCompletedException.class);
    }

    @Test
    void complete_by_assigned_doctor_completes_encounter_cascades_appointment_and_publishes_event() {
        var encounter = encounter();
        var repo = new FakeEncounterRepo(encounter);
        var appointment = Appointment.reconstitute(APPOINTMENT_ID, PATIENT, "Alice", ASSIGNED_DOCTOR, "Dr. Smith",
                Instant.now(), "checkup", AppointmentStatus.IN_PROGRESS);
        var appointmentRepo = new FakeAppointmentRepo(appointment);
        var events = new RecordingDomainEventPublisher();
        var service = new EncounterService(repo, new FakeAccessPolicy(Set.of()), new FakeEncounterNoteRepository(),
                appointmentRepo, events, FIXED_CLOCK);

        var completed = service.complete(encounter.getId(), ASSIGNED_DOCTOR);

        assertThat(completed.getCompletedAt()).isPresent();
        assertThat(appointmentRepo.findById(APPOINTMENT_ID).orElseThrow().getStatus())
                .isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(events.published).hasSize(1);
        var event = events.published.get(0);
        assertThat(event.encounterId()).isEqualTo(encounter.getId());
        assertThat(event.appointmentId()).isEqualTo(APPOINTMENT_ID);
        assertThat(event.patientId()).isEqualTo(PATIENT);
        assertThat(event.doctorId()).isEqualTo(ASSIGNED_DOCTOR);
    }

    @Test
    void complete_records_the_clocks_current_instant_as_completedAt() {
        var encounter = encounter();
        var repo = new FakeEncounterRepo(encounter);
        var appointment = Appointment.reconstitute(APPOINTMENT_ID, PATIENT, "Alice", ASSIGNED_DOCTOR, "Dr. Smith",
                Instant.now(), "checkup", AppointmentStatus.IN_PROGRESS);
        var service = new EncounterService(repo, new FakeAccessPolicy(Set.of()), new FakeEncounterNoteRepository(),
                new FakeAppointmentRepo(appointment), new RecordingDomainEventPublisher(), FIXED_CLOCK);

        var completed = service.complete(encounter.getId(), ASSIGNED_DOCTOR);

        assertThat(completed.getCompletedAt()).contains(FIXED_NOW);
    }

    @Test
    void complete_by_non_assigned_doctor_throws_AccessDeniedException() {
        var encounter = encounter();
        var repo = new FakeEncounterRepo(encounter);
        var service = new EncounterService(repo, new FakeAccessPolicy(Set.of(), true), new FakeEncounterNoteRepository(),
                new FakeAppointmentRepo(), new RecordingDomainEventPublisher(), FIXED_CLOCK);

        assertThatThrownBy(() -> service.complete(encounter.getId(), UNRELATED_DOCTOR))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void complete_on_already_completed_encounter_throws() {
        var completed = Encounter.reconstitute(EncounterId.generate(), APPOINTMENT_ID, ASSIGNED_DOCTOR, PATIENT,
                Instant.now(), List.of());
        var repo = new FakeEncounterRepo(completed);
        var service = new EncounterService(repo, new FakeAccessPolicy(Set.of()), new FakeEncounterNoteRepository(),
                new FakeAppointmentRepo(), new RecordingDomainEventPublisher(), FIXED_CLOCK);

        assertThatThrownBy(() -> service.complete(completed.getId(), ASSIGNED_DOCTOR))
                .isInstanceOf(EncounterCompletedException.class);
    }

    // --- fakes ---

    static class FakeAppointmentRepo implements AppointmentRepository {
        final List<Appointment> store;

        FakeAppointmentRepo(Appointment... appointments) {
            this.store = new ArrayList<>(List.of(appointments));
        }

        @Override
        public boolean existsByDoctorAndPatient(UserId doctorId, PatientId patientId) {
            return false;
        }

        @Override
        public Set<PatientId> getPatientIdsByDoctor(UserId doctorId) {
            return Set.of();
        }

        @Override
        public Appointment save(Appointment appointment) {
            store.removeIf(a -> a.getId().equals(appointment.getId()));
            store.add(appointment);
            return appointment;
        }

        @Override
        public Appointment saveNew(Appointment appointment) {
            store.add(appointment);
            return appointment;
        }

        @Override
        public Optional<Appointment> findById(AppointmentId id) {
            return store.stream().filter(a -> a.getId().equals(id)).findFirst();
        }

        @Override
        public AppointmentPage findAll(AppointmentQuery query) {
            return new AppointmentPage(store, store.size(), query.page(), query.pageSize());
        }
    }

    static class RecordingDomainEventPublisher implements DomainEventPublisher {
        final List<EncounterCompletedEvent> published = new ArrayList<>();

        @Override
        @SuppressWarnings("unchecked")
        public <T> void publish(T event) {
            published.add((EncounterCompletedEvent) event);
        }
    }

    static class FakeEncounterRepo implements EncounterRepository {
        final List<Encounter> store;

        FakeEncounterRepo(Encounter... encounters) {
            this.store = new ArrayList<>(List.of(encounters));
        }

        @Override
        public Encounter saveNew(Encounter encounter) {
            store.add(encounter);
            return encounter;
        }

        @Override
        public Encounter save(Encounter encounter) {
            store.removeIf(e -> e.getId().equals(encounter.getId()));
            store.add(encounter);
            return encounter;
        }

        @Override
        public Optional<Encounter> findById(EncounterId id) {
            return store.stream().filter(e -> e.getId().equals(id)).findFirst();
        }

        @Override
        public Optional<Encounter> findByAppointmentId(AppointmentId appointmentId) {
            return store.stream().filter(e -> e.getAppointmentId().equals(appointmentId)).findFirst();
        }
    }

    static class FakeEncounterNoteRepository implements EncounterNoteRepository {
        private final Map<EncounterId, List<NoteVersion>> store = new HashMap<>();

        @Override
        public NoteVersion save(EncounterId encounterId, NoteVersion note) {
            store.computeIfAbsent(encounterId, id -> new ArrayList<>()).add(note);
            return note;
        }

        @Override
        public List<NoteVersion> findByEncounterId(EncounterId encounterId) {
            return List.copyOf(store.getOrDefault(encounterId, List.of()));
        }
    }

    static class FakeAccessPolicy implements AccessPolicy {
        private final Set<UserId> doctorsWhoMayAccessPatient;
        private final boolean denyCheck;

        FakeAccessPolicy(Set<UserId> doctorsWhoMayAccessPatient) {
            this(doctorsWhoMayAccessPatient, false);
        }

        FakeAccessPolicy(Set<UserId> doctorsWhoMayAccessPatient, boolean denyCheck) {
            this.doctorsWhoMayAccessPatient = doctorsWhoMayAccessPatient;
            this.denyCheck = denyCheck;
        }

        @Override
        public void check(Permission permission, UserId actor, ResourceId resource) {
            if (denyCheck) {
                throw new AccessDeniedException(permission + " denied for " + actor.value());
            }
        }

        @Override
        public boolean isAllowed(Permission permission, UserId actor) {
            return true;
        }

        @Override
        public boolean isDoctor() {
            return true;
        }

        @Override
        public boolean mayAccessOwnedBy(UserId resourceOwner, UserId actor) {
            return resourceOwner.equals(actor);
        }

        @Override
        public boolean mayAccessPatient(UserId actor, PatientId patientId) {
            return doctorsWhoMayAccessPatient.contains(actor);
        }
    }
}
