package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import com.github.database.rider.cdi.api.DBRider;
import com.github.database.rider.core.api.dataset.DataSet;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import net.fmjaeschke.quantumhealth.application.exception.ConcurrentModificationException;
import net.fmjaeschke.quantumhealth.application.ports.out.PrescriptionRepository;
import net.fmjaeschke.quantumhealth.domain.model.MedicationItem;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.Prescription;
import net.fmjaeschke.quantumhealth.domain.model.PrescriptionId;
import net.fmjaeschke.quantumhealth.domain.model.PrescriptionStatus;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import org.hibernate.StaleStateException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
@DBRider
class JpaPrescriptionRepositoryTest {

    private static final PrescriptionId RX_001 = PrescriptionId.of(UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001"));
    private static final PrescriptionId RX_002 = PrescriptionId.of(UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002"));
    private static final PrescriptionId RX_003 = PrescriptionId.of(UUID.fromString("bbbbbbbb-0000-0000-0000-000000000003"));
    private static final PrescriptionId RX_004 = PrescriptionId.of(UUID.fromString("bbbbbbbb-0000-0000-0000-000000000004"));
    private static final PrescriptionId RX_005 = PrescriptionId.of(UUID.fromString("bbbbbbbb-0000-0000-0000-000000000005"));

    @Inject
    PrescriptionRepository repository;

    @Inject
    EntityManager entityManager;

    private static final UUID ALICE_UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    private static final List<MedicationItem> ITEMS = List.of(
            new MedicationItem("Ibuprofen", "400mg", "twice daily"));

    private Prescription newPrescription() {
        return Prescription.issue(
                PatientId.of(ALICE_UUID), "Alice Smith",
                UserId.of("doctor-1"), "Dr. One",
                ITEMS);
    }

    @Test
    @DataSet("datasets/prescription.yml")
    @Transactional
    void saveNew_persists_prescription_and_returns_it_with_id() {
        var saved = repository.saveNew(newPrescription());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(PrescriptionStatus.ISSUED);
        assertThat(saved.getMedications()).hasSize(1);
        assertThat(saved.getMedications().getFirst().drugName()).isEqualTo("Ibuprofen");
        var retrieved = repository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getMedications().getFirst().dosage()).isEqualTo("400mg");
    }

    @Test
    @DataSet("datasets/prescription.yml")
    void findById_returns_seeded_prescription() {
        var found = repository.findById(RX_001);

        assertThat(found).isPresent();
        assertThat(found.get().getDoctorId()).isEqualTo(UserId.of("doctor-1"));
        assertThat(found.get().getPatientName()).isEqualTo("Alice Smith");
        assertThat(found.get().getStatus()).isEqualTo(PrescriptionStatus.ISSUED);
        assertThat(found.get().getMedications()).hasSize(1);
        assertThat(found.get().getMedications().getFirst().drugName()).isEqualTo("Aspirin");
        assertThat(found.get().getMedications().getFirst().frequency()).isEqualTo("once daily");
    }

    @Test
    @DataSet("datasets/prescription.yml")
    void findById_returns_empty_for_unknown_id() {
        var found = repository.findById(PrescriptionId.generate());
        assertThat(found).isEmpty();
    }

    @Test
    @DataSet("datasets/prescription.yml")
    void medications_loaded_from_fixture_parse_correctly() {
        var loaded = repository.findById(RX_001).orElseThrow();

        assertThat(loaded.getMedications()).hasSize(1);
        assertThat(loaded.getMedications().getFirst().drugName()).isEqualTo("Aspirin");
        assertThat(loaded.getMedications().getFirst().dosage()).isEqualTo("100mg");
        assertThat(loaded.getMedications().getFirst().frequency()).isEqualTo("once daily");
    }

    @Test
    @DataSet("datasets/prescription.yml")
    void expired_at_round_trips_correctly_through_persistence() {
        var loaded = repository.findById(RX_003).orElseThrow();

        assertThat(loaded.getStatus()).isEqualTo(PrescriptionStatus.EXPIRED);
        assertThat(loaded.getExpiredAt()).isEqualTo(Instant.parse("2025-02-01T10:00:00Z"));
    }

    @Test
    @DataSet("datasets/prescription.yml")
    @Transactional
    void save_after_fulfill_persists_fulfilled_status_and_audit_fields() {
        var rx = repository.findById(RX_001).orElseThrow();
        var fulfilled = rx.fulfill(UserId.of("pharmacist-1"));

        var updated = repository.save(fulfilled);

        assertThat(updated.getStatus()).isEqualTo(PrescriptionStatus.FULFILLED);
        assertThat(updated.getFulfilledAt()).isNotNull();
        assertThat(updated.getFulfilledBy()).isEqualTo(UserId.of("pharmacist-1"));
        assertThat(updated.getCancelledAt()).isNull();
    }

    @Test
    @DataSet("datasets/prescription.yml")
    @Transactional
    void save_after_cancel_persists_cancelled_status_and_audit_fields() {
        var rx = repository.findById(RX_001).orElseThrow();
        var cancelled = rx.cancel(UserId.of("doctor-1"), "Prescribing error");

        var updated = repository.save(cancelled);

        assertThat(updated.getStatus()).isEqualTo(PrescriptionStatus.CANCELLED);
        assertThat(updated.getCancelledAt()).isNotNull();
        assertThat(updated.getCancelledBy()).isEqualTo(UserId.of("doctor-1"));
        assertThat(updated.getCancelledReason()).isEqualTo("Prescribing error");
        assertThat(updated.getFulfilledAt()).isNull();
    }

    @Test
    @DataSet("datasets/prescription.yml")
    void findStale_returns_only_stale_ISSUED_rows_and_expireOne_marks_them_expired() {
        var threshold = Instant.now().minus(30, ChronoUnit.DAYS);

        var stale = QuarkusTransaction.requiringNew().call(() -> repository.findStale(threshold));
        assertThat(stale).extracting(p -> p.getId().value()).containsExactly(RX_001.value());

        repository.expireOne(stale.getFirst());

        var expired = QuarkusTransaction.requiringNew().call(() -> repository.findById(RX_001).orElseThrow());
        assertThat(expired.getStatus()).isEqualTo(PrescriptionStatus.EXPIRED);
        assertThat(expired.getExpiredAt()).isNotNull();
        var recent = QuarkusTransaction.requiringNew().call(() -> repository.findById(RX_002).orElseThrow());
        assertThat(recent.getStatus()).isEqualTo(PrescriptionStatus.ISSUED);
    }

    @Test
    @DataSet("datasets/prescription.yml")
    void findStale_excludes_FULFILLED_and_CANCELLED_rows() {
        var threshold = Instant.now().minus(30, ChronoUnit.DAYS);

        var stale = QuarkusTransaction.requiringNew().call(() -> repository.findStale(threshold));

        assertThat(stale).extracting(p -> p.getId().value())
                .doesNotContain(RX_004.value(), RX_005.value()).isNotEmpty();
    }

    @Test
    @DataSet("datasets/prescription.yml")
    void save_with_stale_version_throws_concurrent_modification_exception() {
        var snapshot1 = QuarkusTransaction.requiringNew().call(() -> repository.findById(RX_001).orElseThrow());
        var snapshot2 = QuarkusTransaction.requiringNew().call(() -> repository.findById(RX_001).orElseThrow());

        QuarkusTransaction.requiringNew().run(() ->
                repository.save(snapshot1.fulfill(UserId.of("pharmacist-1"))));

        assertThatThrownBy(() -> QuarkusTransaction.requiringNew().run(() ->
                repository.save(snapshot2.cancel(UserId.of("doctor-1"), "stale update"))))
                .isInstanceOf(ConcurrentModificationException.class)
                .hasCauseInstanceOf(StaleStateException.class);
    }

    @Test
    @DataSet("datasets/prescription.yml")
    void list_scoped_to_doctor_returns_only_that_doctors_prescriptions() {
        var page = repository.findAll(0, 20, Optional.of(UserId.of("doctor-1")));

        assertThat(page.prescriptions()).hasSize(5);
        assertThat(page.prescriptions()).allMatch(rx -> rx.getDoctorId().equals(UserId.of("doctor-1")));
    }

    @Test
    @DataSet("datasets/prescription.yml")
    void list_unscoped_returns_all_prescriptions() {
        var page = repository.findAll(0, 20, Optional.empty());

        assertThat(page.prescriptions()).hasSize(6);
    }

    @Test
    @Transactional
    void expiry_index_exists_on_status_and_issued_at() {
        var indexDef = (String) entityManager.createNativeQuery(
                        "SELECT indexdef FROM pg_indexes WHERE indexname = 'idx_prescription_expiry'")
                .getSingleResult();

        assertThat(indexDef)
                .contains("qh_prescription")
                .contains("status")
                .contains("issued_at");
    }

    @Test
    @DataSet("datasets/prescription.yml")
    @Transactional
    void expiry_query_uses_index_scan_not_seq_scan() {
        entityManager.createNativeQuery("SET LOCAL enable_seqscan = off").executeUpdate();

        @SuppressWarnings("unchecked")
        List<String> plan = entityManager.createNativeQuery(
                        "EXPLAIN SELECT * FROM qh_prescription WHERE status = 'ISSUED' AND issued_at < now()")
                .getResultList();

        var planText = String.join("\n", plan);
        assertThat(planText).contains("Index Scan");
        assertThat(planText).doesNotContain("Seq Scan on qh_prescription");
    }
}
