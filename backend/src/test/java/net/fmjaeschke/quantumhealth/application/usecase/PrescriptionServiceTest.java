package net.fmjaeschke.quantumhealth.application.usecase;

import net.fmjaeschke.quantumhealth.domain.model.Permission;
import net.fmjaeschke.quantumhealth.application.exception.AccessDeniedException;
import net.fmjaeschke.quantumhealth.application.exception.DoctorNotFoundException;
import net.fmjaeschke.quantumhealth.application.exception.PatientNotFoundException;
import net.fmjaeschke.quantumhealth.application.exception.PrescriptionNotFoundException;
import net.fmjaeschke.quantumhealth.application.ports.out.AccessPolicy;
import net.fmjaeschke.quantumhealth.application.ports.out.DoctorPort;
import net.fmjaeschke.quantumhealth.application.ports.out.PatientRepository;
import net.fmjaeschke.quantumhealth.application.ports.out.PrescriptionRepository;
import net.fmjaeschke.quantumhealth.domain.model.Doctor;
import net.fmjaeschke.quantumhealth.domain.model.MedicationItem;
import net.fmjaeschke.quantumhealth.domain.model.Patient;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.PatientPage;
import net.fmjaeschke.quantumhealth.domain.model.PatientQuery;
import net.fmjaeschke.quantumhealth.domain.model.Disposition;
import net.fmjaeschke.quantumhealth.domain.model.Prescription;
import net.fmjaeschke.quantumhealth.domain.model.PrescriptionId;
import net.fmjaeschke.quantumhealth.domain.model.PrescriptionPage;
import net.fmjaeschke.quantumhealth.domain.model.PrescriptionStatus;
import net.fmjaeschke.quantumhealth.domain.model.ResourceId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import org.junit.jupiter.api.Test;

import jakarta.persistence.OptimisticLockException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrescriptionServiceTest {

    static final UserId ACTOR = UserId.of("dr-smith");
    static final PatientId PATIENT_ID = PatientId.generate();
    static final String CANCEL_REASON = "Patient request";
    static final List<MedicationItem> MEDICATIONS = List.of(new MedicationItem("Aspirin", "100mg", "daily"));

    static final Patient ALICE = Patient.reconstitute(PATIENT_ID, "Alice", "Smith", LocalDate.of(1990, 1, 1));
    static final Doctor DR_SMITH = new Doctor(ACTOR, "Dr. Smith");

    private static PrescriptionService service(FakePrescriptionRepo repo, boolean isDoctor) {
        return service(repo, isDoctor, false);
    }

    private static PrescriptionService service(FakePrescriptionRepo repo, boolean isDoctor, boolean denyCheck) {
        return service(repo, new FakeAccessPolicy(isDoctor, denyCheck));
    }

    private static PrescriptionService service(FakePrescriptionRepo repo, FakeAccessPolicy policy) {
        return new PrescriptionService(repo, new FakePatientRepo(ALICE), new FakeDoctorPort(DR_SMITH), policy);
    }

    // --- issue() ---

    @Test
    void issue_resolves_patient_and_doctor_names_and_returns_issued_prescription() {
        var repo = new FakePrescriptionRepo();
        var result = service(repo, false).issue(ACTOR, PATIENT_ID, MEDICATIONS);

        assertThat(result.getStatus()).isEqualTo(PrescriptionStatus.ISSUED);
        assertThat(result.getPatientName()).isEqualTo("Alice Smith");
        assertThat(result.getDoctorName()).isEqualTo("Dr. Smith");
        assertThat(result.getMedications()).isEqualTo(MEDICATIONS);
    }

    @Test
    void issue_throws_PatientNotFoundException_when_patient_not_found() {
        var service = new PrescriptionService(new FakePrescriptionRepo(), new FakePatientRepo(), new FakeDoctorPort(DR_SMITH), new FakeAccessPolicy(false));

        assertThatThrownBy(() -> service.issue(ACTOR, PATIENT_ID, MEDICATIONS))
                .isInstanceOf(PatientNotFoundException.class);
    }

    @Test
    void issue_throws_DoctorNotFoundException_when_doctor_not_found() {
        var service = new PrescriptionService(new FakePrescriptionRepo(), new FakePatientRepo(ALICE), new FakeDoctorPort(), new FakeAccessPolicy(false));

        assertThatThrownBy(() -> service.issue(ACTOR, PATIENT_ID, MEDICATIONS))
                .isInstanceOf(DoctorNotFoundException.class);
    }

    // --- findById() ---

    @Test
    void findById_returns_prescription_when_doctor_is_owner() {
        var prescription = Prescription.issue(PATIENT_ID, "Alice Smith", ACTOR, "Dr. Smith", MEDICATIONS);
        var result = service(new FakePrescriptionRepo(prescription), true).findById(prescription.getId(), ACTOR);

        assertThat(result.getId()).isEqualTo(prescription.getId());
    }

    @Test
    void findById_throws_PrescriptionNotFoundException_when_doctor_is_not_owner() {
        var prescription = Prescription.issue(PATIENT_ID, "Alice Smith", UserId.of("dr-other"), "Dr. Other", MEDICATIONS);
        var repo = new FakePrescriptionRepo(prescription);

        assertThatThrownBy(() -> service(repo, true).findById(prescription.getId(), ACTOR))
                .isInstanceOf(PrescriptionNotFoundException.class);
    }

    @Test
    void findById_returns_prescription_for_non_doctor_regardless_of_owner() {
        var prescription = Prescription.issue(PATIENT_ID, "Alice Smith", UserId.of("dr-other"), "Dr. Other", MEDICATIONS);
        var result = service(new FakePrescriptionRepo(prescription), false).findById(prescription.getId(), ACTOR);

        assertThat(result.getId()).isEqualTo(prescription.getId());
    }

    @Test
    void findById_throws_PrescriptionNotFoundException_when_not_found() {
        var id = PrescriptionId.generate();

        assertThatThrownBy(() -> service(new FakePrescriptionRepo(), false).findById(id, ACTOR))
                .isInstanceOf(PrescriptionNotFoundException.class);
    }

    // --- fulfill() ---

    @Test
    void fulfill_returns_fulfilled_prescription() {
        var prescription = Prescription.issue(PATIENT_ID, "Alice Smith", ACTOR, "Dr. Smith", MEDICATIONS);
        var result = service(new FakePrescriptionRepo(prescription), false).fulfill(prescription.getId(), ACTOR);

        assertThat(result.getStatus()).isEqualTo(PrescriptionStatus.FULFILLED);
        assertThat(result.getFulfilledBy()).isEqualTo(ACTOR);
    }

    @Test
    void fulfill_throws_PrescriptionNotFoundException_when_not_found() {
        var id = PrescriptionId.generate();

        assertThatThrownBy(() -> service(new FakePrescriptionRepo(), false).fulfill(id, ACTOR))
                .isInstanceOf(PrescriptionNotFoundException.class);
    }

    @Test
    void fulfill_denies_when_access_policy_denies() {
        var prescription = Prescription.issue(PATIENT_ID, "Alice Smith", ACTOR, "Dr. Smith", MEDICATIONS);
        var policy = new FakeAccessPolicy(false, true);
        var service = service(new FakePrescriptionRepo(prescription), policy);

        assertThatThrownBy(() -> service.fulfill(prescription.getId(), ACTOR))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(policy.lastPermission).isEqualTo(Permission.DISPENSE_MEDICATION);
    }

    // --- cancel() ---

    @Test
    void cancel_returns_cancelled_prescription_with_reason() {
        var prescription = Prescription.issue(PATIENT_ID, "Alice Smith", ACTOR, "Dr. Smith", MEDICATIONS);
        var result = service(new FakePrescriptionRepo(prescription), false).cancel(prescription.getId(), ACTOR, CANCEL_REASON);

        assertThat(result.getStatus()).isEqualTo(PrescriptionStatus.CANCELLED);
        assertThat(result.getCancelledBy()).isEqualTo(ACTOR);
        assertThat(result.getCancelledReason()).isEqualTo(CANCEL_REASON);
    }

    @Test
    void cancel_throws_PrescriptionNotFoundException_when_not_found() {
        var id = PrescriptionId.generate();

        assertThatThrownBy(() -> service(new FakePrescriptionRepo(), false).cancel(id, ACTOR, CANCEL_REASON))
                .isInstanceOf(PrescriptionNotFoundException.class);
    }

    @Test
    void cancel_denies_when_access_policy_denies() {
        var prescription = Prescription.issue(PATIENT_ID, "Alice Smith", ACTOR, "Dr. Smith", MEDICATIONS);
        var policy = new FakeAccessPolicy(false, true);
        var service = service(new FakePrescriptionRepo(prescription), policy);

        assertThatThrownBy(() -> service.cancel(prescription.getId(), ACTOR, CANCEL_REASON))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(policy.lastPermission).isEqualTo(Permission.CANCEL_PRESCRIPTION);
    }

    // --- list() ---

    @Test
    void list_for_doctor_passes_actor_as_scope_to_repository() {
        var prescription = Prescription.issue(PATIENT_ID, "Alice Smith", ACTOR, "Dr. Smith", MEDICATIONS);
        var repo = new FakePrescriptionRepo(prescription);
        service(repo, true).list(0, 20, ACTOR);

        assertThat(repo.lastDoctorScope).contains(ACTOR);
    }

    @Test
    void list_for_non_doctor_passes_empty_scope_to_repository() {
        var prescription = Prescription.issue(PATIENT_ID, "Alice Smith", ACTOR, "Dr. Smith", MEDICATIONS);
        var repo = new FakePrescriptionRepo(prescription);
        service(repo, false).list(0, 20, ACTOR);

        assertThat(repo.lastDoctorScope).isEmpty();
    }

    // --- expireOlderThan() ---

    private Prescription staleIssuedPrescription() {
        return Prescription.reconstitute(
                PrescriptionId.generate(),
                PATIENT_ID, "Alice Smith",
                ACTOR, "Dr. Smith",
                MEDICATIONS,
                Instant.now().minus(31, ChronoUnit.DAYS),
                Disposition.issued(),
                0L);
    }

    @Test
    void expireOlderThan_expires_all_stale_prescriptions() {
        var rxA = staleIssuedPrescription();
        var rxB = staleIssuedPrescription();
        var repo = new FakePrescriptionRepo(rxA, rxB);
        repo.staleForExpiry = List.of(rxA, rxB);

        var count = service(repo, false).expireOlderThan(Instant.now());

        assertThat(count).isEqualTo(2);
        assertThat(repo.findById(rxA.getId()).orElseThrow().getStatus()).isEqualTo(PrescriptionStatus.EXPIRED);
        assertThat(repo.findById(rxB.getId()).orElseThrow().getStatus()).isEqualTo(PrescriptionStatus.EXPIRED);
    }

    @Test
    void expireOlderThan_skips_prescription_with_optimistic_lock_conflict_and_continues() {
        var rxA = staleIssuedPrescription();
        var rxB = staleIssuedPrescription();
        var repo = new FakePrescriptionRepo(rxA, rxB);
        repo.staleForExpiry = List.of(rxA, rxB);
        repo.conflictOnExpire = Set.of(rxA.getId());

        var count = service(repo, false).expireOlderThan(Instant.now());

        assertThat(count).isEqualTo(1);
        assertThat(repo.findById(rxA.getId()).orElseThrow().getStatus()).isEqualTo(PrescriptionStatus.ISSUED);
        assertThat(repo.findById(rxB.getId()).orElseThrow().getStatus()).isEqualTo(PrescriptionStatus.EXPIRED);
    }

    // --- fakes ---

    static class FakePrescriptionRepo implements PrescriptionRepository {
        final List<Prescription> store;
        Optional<UserId> lastDoctorScope;
        List<Prescription> staleForExpiry = List.of();
        Set<PrescriptionId> conflictOnExpire = new HashSet<>();

        FakePrescriptionRepo(Prescription... prescriptions) {
            this.store = new ArrayList<>(List.of(prescriptions));
        }

        @Override
        public Prescription saveNew(Prescription prescription) {
            store.add(prescription);
            return prescription;
        }

        @Override
        public Prescription save(Prescription prescription) {
            store.removeIf(p -> p.getId().equals(prescription.getId()));
            store.add(prescription);
            return prescription;
        }

        @Override
        public Optional<Prescription> findById(PrescriptionId id) {
            return store.stream().filter(p -> p.getId().equals(id)).findFirst();
        }

        @Override
        public PrescriptionPage findAll(int page, int pageSize, Optional<UserId> doctorId) {
            this.lastDoctorScope = doctorId;
            var filtered = store.stream()
                    .filter(p -> doctorId.isEmpty() || p.getDoctorId().equals(doctorId.get()))
                    .toList();
            return new PrescriptionPage(filtered, filtered.size(), page, pageSize);
        }

        @Override
        public List<Prescription> findStale(Instant threshold) {
            return staleForExpiry;
        }

        @Override
        public void expireOne(Prescription prescription) {
            if (conflictOnExpire.contains(prescription.getId())) {
                throw new OptimisticLockException("stale version for " + prescription.getId().value());
            }
            save(prescription.expire());
        }
    }

    static class FakeAccessPolicy implements AccessPolicy {
        private final boolean isDoctor;
        private final boolean denyCheck;
        Permission lastPermission;

        FakeAccessPolicy(boolean isDoctor) {
            this(isDoctor, false);
        }

        FakeAccessPolicy(boolean isDoctor, boolean denyCheck) {
            this.isDoctor = isDoctor;
            this.denyCheck = denyCheck;
        }

        @Override
        public void check(Permission permission, UserId actor, ResourceId resource) {
            this.lastPermission = permission;
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
            return isDoctor;
        }

        @Override
        public boolean mayAccessOwnedBy(UserId resourceOwner, UserId actor) {
            return !isDoctor || resourceOwner.equals(actor);
        }

        @Override
        public boolean mayAccessPatient(UserId actor, PatientId patientId) {
            return !isDoctor;
        }
    }

    static class FakePatientRepo implements PatientRepository {
        private final List<Patient> store;

        FakePatientRepo(Patient... patients) {
            this.store = List.of(patients);
        }

        @Override
        public PatientId save(Patient patient) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Patient> findById(PatientId id) {
            return store.stream().filter(p -> p.getId().equals(id)).findFirst();
        }

        @Override
        public PatientPage findAll(PatientQuery query) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PatientPage findByDoctor(UserId doctorId, PatientQuery query) {
            throw new UnsupportedOperationException();
        }
    }

    static class FakeDoctorPort implements DoctorPort {
        private final List<Doctor> store;

        FakeDoctorPort(Doctor... doctors) {
            this.store = List.of(doctors);
        }

        @Override
        public List<Doctor> findByRole(String role) {
            return store;
        }

        @Override
        public Optional<Doctor> findById(UserId id) {
            return store.stream().filter(d -> d.id().equals(id)).findFirst();
        }
    }
}
