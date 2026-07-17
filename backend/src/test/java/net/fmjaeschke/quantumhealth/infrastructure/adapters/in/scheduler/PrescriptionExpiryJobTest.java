package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.scheduler;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import net.fmjaeschke.quantumhealth.application.exception.ConcurrentModificationException;
import net.fmjaeschke.quantumhealth.application.ports.out.PrescriptionRepository;
import net.fmjaeschke.quantumhealth.domain.model.Disposition;
import net.fmjaeschke.quantumhealth.domain.model.MedicationItem;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.Prescription;
import net.fmjaeschke.quantumhealth.domain.model.PrescriptionId;
import net.fmjaeschke.quantumhealth.domain.model.PrescriptionStatus;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
class PrescriptionExpiryJobTest {

    @Inject
    PrescriptionExpiryJob job;

    @Inject
    PrescriptionRepository repository;

    private static final UUID ALICE_UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    private static final List<MedicationItem> ITEMS = List.of(
            new MedicationItem("Ibuprofen", "400mg", "twice daily"));

    private Prescription prescriptionWithIssuedAt(Instant issuedAt) {
        return Prescription.reconstitute(
                PrescriptionId.generate(),
                PatientId.of(ALICE_UUID), "Alice Smith",
                UserId.of("doctor-1"), "Dr. One",
                ITEMS,
                issuedAt,
                Disposition.issued(),
                null);
    }

    @Test
    void expireStale_transitions_only_prescriptions_older_than_30_days_to_EXPIRED() {
        var oldRx = QuarkusTransaction.requiringNew().call(() ->
                repository.saveNew(prescriptionWithIssuedAt(Instant.now().minus(31, ChronoUnit.DAYS))));
        var recentRx = QuarkusTransaction.requiringNew().call(() ->
                repository.saveNew(prescriptionWithIssuedAt(Instant.now().minus(1, ChronoUnit.DAYS))));

        job.expireStale();

        var expiredRx = QuarkusTransaction.requiringNew().call(() -> repository.findById(oldRx.getId()).orElseThrow());
        assertThat(expiredRx.getStatus()).isEqualTo(PrescriptionStatus.EXPIRED);
        assertThat(expiredRx.getExpiredAt()).isNotNull();
        var recentResult = QuarkusTransaction.requiringNew().call(() -> repository.findById(recentRx.getId()).orElseThrow());
        assertThat(recentResult.getStatus()).isEqualTo(PrescriptionStatus.ISSUED);
    }

    @Test
    void expireStale_skips_concurrently_modified_prescription_and_processes_remaining_batch() {
        var rxA = QuarkusTransaction.requiringNew().call(() ->
                repository.saveNew(prescriptionWithIssuedAt(Instant.now().minus(31, ChronoUnit.DAYS))));
        var rxB = QuarkusTransaction.requiringNew().call(() ->
                repository.saveNew(prescriptionWithIssuedAt(Instant.now().minus(31, ChronoUnit.DAYS))));

        // Snapshot of rxA as the expiry job would have read it at the start of a run
        var staleSnapshotA = QuarkusTransaction.requiringNew().call(() -> repository.findById(rxA.getId()).orElseThrow());

        // rxA is fulfilled concurrently after that snapshot was taken
        QuarkusTransaction.requiringNew().run(() -> {
            var fresh = repository.findById(rxA.getId()).orElseThrow();
            repository.save(fresh.fulfill(UserId.of("pharmacist-1")));
        });

        // Expiring the now-stale snapshot conflicts with the concurrent fulfillment ...
        assertThatThrownBy(() -> repository.expireOne(staleSnapshotA))
                .isInstanceOf(ConcurrentModificationException.class);

        // ... and a normal expiry run still processes the rest of the batch correctly
        job.expireStale();

        var resultA = QuarkusTransaction.requiringNew().call(() -> repository.findById(rxA.getId()).orElseThrow());
        assertThat(resultA.getStatus()).isEqualTo(PrescriptionStatus.FULFILLED);
        var resultB = QuarkusTransaction.requiringNew().call(() -> repository.findById(rxB.getId()).orElseThrow());
        assertThat(resultB.getStatus()).isEqualTo(PrescriptionStatus.EXPIRED);
    }
}
