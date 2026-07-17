package net.fmjaeschke.quantumhealth.domain.model;

import net.fmjaeschke.quantumhealth.domain.exception.InvalidPrescriptionStateException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrescriptionTest {

    private static final PatientId PATIENT = PatientId.of(UUID.randomUUID());
    private static final UserId DOCTOR = UserId.of("dr-smith");
    private static final UserId PHARMACIST = UserId.of("pharmacist-1");
    private static final List<MedicationItem> ITEMS = List.of(
            new MedicationItem("Aspirin", "100mg", "once daily"));
    private static final Instant ISSUED_AT = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant FULFILLED_AT = Instant.parse("2026-01-02T10:00:00Z");
    private static final Instant CANCELLED_AT = Instant.parse("2026-01-03T10:00:00Z");
    private static final Instant EXPIRED_AT = Instant.parse("2026-01-31T00:00:00Z");

    @Test
    void issue_creates_prescription_with_issued_status_and_issuedAt() {
        var rx = Prescription.issue(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", ITEMS, ISSUED_AT);

        assertThat(rx.getStatus()).isEqualTo(PrescriptionStatus.ISSUED);
        assertThat(rx.getIssuedAt()).isNotNull();
        assertThat(rx.getId()).isNotNull();
        assertThat(rx.getMedications()).containsExactlyElementsOf(ITEMS);
    }

    @Test
    void issue_records_the_exact_instant_passed_in_as_issuedAt() {
        var rx = Prescription.issue(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", ITEMS, ISSUED_AT);

        assertThat(rx.getIssuedAt()).isEqualTo(ISSUED_AT);
    }

    @Test
    void fulfill_on_issued_prescription_returns_fulfilled_instance_with_audit_fields() {
        var rx = Prescription.issue(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", ITEMS, ISSUED_AT);

        var fulfilled = rx.fulfill(PHARMACIST, FULFILLED_AT);

        assertThat(fulfilled.getStatus()).isEqualTo(PrescriptionStatus.FULFILLED);
        assertThat(fulfilled.getFulfilledAt()).isNotNull();
        assertThat(fulfilled.getFulfilledBy()).isEqualTo(PHARMACIST);
        assertThat(fulfilled.getId()).isEqualTo(rx.getId());
    }

    @Test
    void fulfill_records_the_exact_instant_passed_in_as_fulfilledAt() {
        var rx = Prescription.issue(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", ITEMS, ISSUED_AT);

        var fulfilled = rx.fulfill(PHARMACIST, FULFILLED_AT);

        assertThat(fulfilled.getFulfilledAt()).isEqualTo(FULFILLED_AT);
    }

    @Test
    void fulfill_on_issued_returns_new_instance_not_mutating_original() {
        var rx = Prescription.issue(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", ITEMS, ISSUED_AT);

        rx.fulfill(PHARMACIST, FULFILLED_AT);

        assertThat(rx.getStatus()).isEqualTo(PrescriptionStatus.ISSUED);
    }

    @Test
    void fulfill_on_fulfilled_prescription_throws() {
        var rx = Prescription.issue(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", ITEMS, ISSUED_AT)
                .fulfill(PHARMACIST, FULFILLED_AT);

        assertThatThrownBy(() -> rx.fulfill(PHARMACIST, FULFILLED_AT))
                .isInstanceOf(InvalidPrescriptionStateException.class);
    }

    @Test
    void cancel_on_issued_prescription_returns_cancelled_instance_with_audit_fields() {
        var rx = Prescription.issue(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", ITEMS, ISSUED_AT);

        var cancelled = rx.cancel(DOCTOR, "Prescribing error", CANCELLED_AT);

        assertThat(cancelled.getStatus()).isEqualTo(PrescriptionStatus.CANCELLED);
        assertThat(cancelled.getCancelledAt()).isNotNull();
        assertThat(cancelled.getCancelledBy()).isEqualTo(DOCTOR);
        assertThat(cancelled.getCancelledReason()).isEqualTo("Prescribing error");
        assertThat(cancelled.getId()).isEqualTo(rx.getId());
    }

    @Test
    void cancel_records_the_exact_instant_passed_in_as_cancelledAt() {
        var rx = Prescription.issue(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", ITEMS, ISSUED_AT);

        var cancelled = rx.cancel(DOCTOR, "Prescribing error", CANCELLED_AT);

        assertThat(cancelled.getCancelledAt()).isEqualTo(CANCELLED_AT);
    }

    @Test
    void cancel_on_fulfilled_prescription_throws() {
        var rx = Prescription.issue(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", ITEMS, ISSUED_AT)
                .fulfill(PHARMACIST, FULFILLED_AT);

        assertThatThrownBy(() -> rx.cancel(DOCTOR, "Late cancel", CANCELLED_AT))
                .isInstanceOf(InvalidPrescriptionStateException.class);
    }

    @Test
    void isFulfillable_returns_true_only_for_issued_status() {
        var issued = Prescription.issue(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", ITEMS, ISSUED_AT);
        var fulfilled = issued.fulfill(PHARMACIST, FULFILLED_AT);
        var cancelled = Prescription.issue(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", ITEMS, ISSUED_AT)
                .cancel(DOCTOR, "reason", CANCELLED_AT);

        assertThat(issued.isFulfillable()).isTrue();
        assertThat(fulfilled.isFulfillable()).isFalse();
        assertThat(cancelled.isFulfillable()).isFalse();
    }

    @Test
    void expire_on_issued_prescription_returns_expired_instance_with_expiredAt() {
        var rx = Prescription.issue(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", ITEMS, ISSUED_AT);

        var expired = rx.expire(EXPIRED_AT);

        assertThat(expired.getStatus()).isEqualTo(PrescriptionStatus.EXPIRED);
        assertThat(expired.getExpiredAt()).isNotNull();
        assertThat(expired.getId()).isEqualTo(rx.getId());
    }

    @Test
    void expire_records_the_exact_instant_passed_in_as_expiredAt() {
        var rx = Prescription.issue(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", ITEMS, ISSUED_AT);

        var expired = rx.expire(EXPIRED_AT);

        assertThat(expired.getExpiredAt()).isEqualTo(EXPIRED_AT);
    }

    @Test
    void expire_on_issued_returns_new_instance_not_mutating_original() {
        var rx = Prescription.issue(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", ITEMS, ISSUED_AT);

        rx.expire(EXPIRED_AT);

        assertThat(rx.getStatus()).isEqualTo(PrescriptionStatus.ISSUED);
    }

    @Test
    void expire_on_non_issued_prescription_throws() {
        var rx = Prescription.issue(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", ITEMS, ISSUED_AT)
                .fulfill(PHARMACIST, FULFILLED_AT);

        assertThatThrownBy(() -> rx.expire(EXPIRED_AT))
                .isInstanceOf(InvalidPrescriptionStateException.class);
    }

    @Test
    void isExpirable_returns_true_only_for_issued_status() {
        var issued = Prescription.issue(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", ITEMS, ISSUED_AT);
        var fulfilled = issued.fulfill(PHARMACIST, FULFILLED_AT);
        var cancelled = Prescription.issue(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", ITEMS, ISSUED_AT)
                .cancel(DOCTOR, "reason", CANCELLED_AT);

        assertThat(issued.isExpirable()).isTrue();
        assertThat(fulfilled.isExpirable()).isFalse();
        assertThat(cancelled.isExpirable()).isFalse();
    }
}
