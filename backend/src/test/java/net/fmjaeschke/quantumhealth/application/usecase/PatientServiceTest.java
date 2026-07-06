package net.fmjaeschke.quantumhealth.application.usecase;

import net.fmjaeschke.quantumhealth.domain.model.Permission;
import net.fmjaeschke.quantumhealth.application.exception.AccessDeniedException;
import net.fmjaeschke.quantumhealth.application.exception.PatientNotFoundException;
import net.fmjaeschke.quantumhealth.application.ports.out.AccessPolicy;
import net.fmjaeschke.quantumhealth.application.ports.out.PatientRepository;
import net.fmjaeschke.quantumhealth.domain.model.Patient;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.PatientPage;
import net.fmjaeschke.quantumhealth.domain.model.PatientQuery;
import net.fmjaeschke.quantumhealth.domain.model.ResourceId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PatientServiceTest {

    private static final UserId CLERK = UserId.of("clerk-1");
    private static final UserId DOCTOR = UserId.of("doctor-1");

    @Test
    void register_saves_patient_and_returns_it() {
        var repo = new FakePatientRepository();
        var service = new PatientService(repo, new AlwaysAllowPolicy());

        var patient = service.register(CLERK, "Alice", "Smith", LocalDate.of(1990, 5, 15));

        assertThat(patient.getId()).isNotNull();
        assertThat(patient.getFirstName()).isEqualTo("Alice");
        assertThat(repo.getSaved()).hasSize(1);
    }

    @Test
    void read_returns_patient_when_access_allowed() {
        var patient = Patient.reconstitute(PatientId.generate(), "Alice", "Smith", LocalDate.of(1990, 5, 15));
        var service = new PatientService(new FoundPatientRepository(patient), new AlwaysAllowPolicy());

        var found = service.findById(patient.getId(), CLERK);

        assertThat(found.getFirstName()).isEqualTo("Alice");
    }

    @Test
    void read_throws_PatientNotFoundException_when_doctor_never_treated_patient() {
        var patient = Patient.reconstitute(PatientId.generate(), "Alice", "Smith", LocalDate.of(1990, 5, 15));
        var service = new PatientService(new FoundPatientRepository(patient), new AlwaysDenyPolicy());

        assertThatThrownBy(() -> service.findById(patient.getId(), DOCTOR)).isInstanceOf(PatientNotFoundException.class);
    }

    @Test
    void read_throws_when_patient_not_found() {
        var service = new PatientService(new EmptyRepository(), new AlwaysAllowPolicy());

        assertThatThrownBy(() -> service.findById(PatientId.generate(), CLERK)).isInstanceOf(PatientNotFoundException.class);
    }

    @Test
    void listPatients_returns_all_patients_for_non_doctor() {
        var alice = Patient.reconstitute(PatientId.generate(), "Alice", "Smith", LocalDate.of(1990, 5, 15));
        var repo = new FoundPatientRepository(alice);
        var service = new PatientService(repo, new AlwaysAllowPolicy());
        var query = new PatientQuery(Optional.empty(), Optional.empty(), 0, 20, PatientQuery.SortField.LAST_NAME, PatientQuery.SortDirection.ASC);

        var page = service.listPatients(CLERK, query);

        assertThat(page.patients()).hasSize(1);
        assertThat(page.patients()
                .getFirst()
                .getFirstName()).isEqualTo("Alice");
    }

    @Test
    void listPatients_scopes_to_doctor_patients() {
        var alice = Patient.reconstitute(PatientId.generate(), "Alice", "Smith", LocalDate.of(1990, 5, 15));
        var repo = new FoundPatientRepository(alice);
        var service = new PatientService(repo, new DoctorPolicy());
        var query = new PatientQuery(Optional.empty(), Optional.empty(), 0, 20, PatientQuery.SortField.LAST_NAME, PatientQuery.SortDirection.ASC);

        var page = service.listPatients(DOCTOR, query);

        assertThat(page.patients()).hasSize(1);
    }

    // --- fakes ---

    static class AlwaysAllowPolicy implements AccessPolicy {
        @Override
        public void check(Permission p, UserId actor, ResourceId resource) {
            // Not implemented
        }

        @Override
        public boolean isAllowed(Permission p, UserId actor) {
            return true;
        }

        @Override
        public boolean isDoctor() {
            return false;
        }

        @Override
        public boolean mayAccessOwnedBy(UserId resourceOwner, UserId actor) {
            return true;
        }

        @Override
        public boolean mayAccessPatient(UserId actor, PatientId patientId) {
            return true;
        }
    }

    static class AlwaysDenyPolicy implements AccessPolicy {
        @Override
        public void check(Permission p, UserId actor, ResourceId resource) {
            throw new AccessDeniedException(p.name());
        }

        @Override
        public boolean isAllowed(Permission p, UserId actor) {
            return false;
        }

        @Override
        public boolean isDoctor() {
            return false;
        }

        @Override
        public boolean mayAccessOwnedBy(UserId resourceOwner, UserId actor) {
            return true;
        }

        @Override
        public boolean mayAccessPatient(UserId actor, PatientId patientId) {
            return false;
        }
    }

    static class DoctorPolicy implements AccessPolicy {
        @Override
        public void check(Permission p, UserId actor, ResourceId resource) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isAllowed(Permission p, UserId actor) {
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
            return true;
        }
    }

    static class FakePatientRepository implements PatientRepository {
        private final List<Patient> saved = new ArrayList<>();

        @Override
        public PatientId save(Patient patient) {
            saved.add(patient);
            return patient.getId();
        }

        @Override
        public Optional<Patient> findById(PatientId id) {
            return saved.stream()
                    .filter(p -> p.getId()
                            .equals(id))
                    .findFirst();
        }

        @Override
        public PatientPage findAll(PatientQuery q) {
            return new PatientPage(saved, saved.size(), q.page(), q.size());
        }

        @Override
        public PatientPage findByDoctor(UserId d, PatientQuery q) {
            return new PatientPage(saved, saved.size(), q.page(), q.size());
        }

        List<Patient> getSaved() {
            return saved;
        }
    }

    static class FoundPatientRepository implements PatientRepository {
        private final Patient patient;

        FoundPatientRepository(Patient patient) {
            this.patient = patient;
        }

        @Override
        public PatientId save(Patient p) {
            return p.getId();
        }

        @Override
        public Optional<Patient> findById(PatientId id) {
            return Optional.of(patient);
        }

        @Override
        public PatientPage findAll(PatientQuery q) {
            return new PatientPage(List.of(patient), 1, q.page(), q.size());
        }

        @Override
        public PatientPage findByDoctor(UserId d, PatientQuery q) {
            return new PatientPage(List.of(patient), 1, q.page(), q.size());
        }
    }

    static class EmptyRepository implements PatientRepository {
        @Override
        public PatientId save(Patient p) {
            return p.getId();
        }

        @Override
        public Optional<Patient> findById(PatientId id) {
            return Optional.empty();
        }

        @Override
        public PatientPage findAll(PatientQuery q) {
            return new PatientPage(List.of(), 0, q.page(), q.size());
        }

        @Override
        public PatientPage findByDoctor(UserId d, PatientQuery q) {
            return new PatientPage(List.of(), 0, q.page(), q.size());
        }
    }
}
