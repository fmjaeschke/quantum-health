package net.fmjaeschke.quantumhealth.domain.model;

import net.fmjaeschke.quantumhealth.domain.exception.InvalidPrescriptionStateException;
import org.junit.jupiter.api.Test;

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

    @Test
    void issue_creates_prescription_with_issued_status_and_issuedAt() {
        var rx = Prescription.issue(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", ITEMS);

        assertThat(rx.getStatus()).isEqualTo(PrescriptionStatus.ISSUED);
        assertThat(rx.getIssuedAt()).isNotNull();
        assertThat(rx.getId()).isNotNull();
        assertThat(rx.getMedications()).containsExactlyElementsOf(ITEMS);
    }

    @Test
    void fulfill_on_issued_prescription_returns_fulfilled_instance_with_audit_fields() {
        var rx = Prescription.issue(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", ITEMS);

        var fulfilled = rx.fulfill(PHARMACIST);

        assertThat(fulfilled.getStatus()).isEqualTo(PrescriptionStatus.FULFILLED);
        assertThat(fulfilled.getFulfilledAt()).isNotNull();
        assertThat(fulfilled.getFulfilledBy()).isEqualTo(PHARMACIST);
        assertThat(fulfilled.getId()).isEqualTo(rx.getId());
    }

    @Test
    void fulfill_on_issued_returns_new_instance_not_mutating_original() {
        var rx = Prescription.issue(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", ITEMS);

        rx.fulfill(PHARMACIST);

        assertThat(rx.getStatus()).isEqualTo(PrescriptionStatus.ISSUED);
    }

    @Test
    void fulfill_on_fulfilled_prescription_throws() {
        var rx = Prescription.issue(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", ITEMS)
                .fulfill(PHARMACIST);

        assertThatThrownBy(() -> rx.fulfill(PHARMACIST))
                .isInstanceOf(InvalidPrescriptionStateException.class);
    }

    @Test
    void cancel_on_issued_prescription_returns_cancelled_instance_with_audit_fields() {
        var rx = Prescription.issue(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", ITEMS);

        var cancelled = rx.cancel(DOCTOR, "Prescribing error");

        assertThat(cancelled.getStatus()).isEqualTo(PrescriptionStatus.CANCELLED);
        assertThat(cancelled.getCancelledAt()).isNotNull();
        assertThat(cancelled.getCancelledBy()).isEqualTo(DOCTOR);
        assertThat(cancelled.getCancelledReason()).isEqualTo("Prescribing error");
        assertThat(cancelled.getId()).isEqualTo(rx.getId());
    }

    @Test
    void cancel_on_fulfilled_prescription_throws() {
        var rx = Prescription.issue(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", ITEMS)
                .fulfill(PHARMACIST);

        assertThatThrownBy(() -> rx.cancel(DOCTOR, "Late cancel"))
                .isInstanceOf(InvalidPrescriptionStateException.class);
    }

    @Test
    void isFulfillable_returns_true_only_for_issued_status() {
        var issued = Prescription.issue(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", ITEMS);
        var fulfilled = issued.fulfill(PHARMACIST);
        var cancelled = Prescription.issue(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", ITEMS)
                .cancel(DOCTOR, "reason");

        assertThat(issued.isFulfillable()).isTrue();
        assertThat(fulfilled.isFulfillable()).isFalse();
        assertThat(cancelled.isFulfillable()).isFalse();
    }

    @Test
    void expire_on_issued_prescription_returns_expired_instance_with_expiredAt() {
        var rx = Prescription.issue(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", ITEMS);

        var expired = rx.expire();

        assertThat(expired.getStatus()).isEqualTo(PrescriptionStatus.EXPIRED);
        assertThat(expired.getExpiredAt()).isNotNull();
        assertThat(expired.getId()).isEqualTo(rx.getId());
    }

    @Test
    void expire_on_issued_returns_new_instance_not_mutating_original() {
        var rx = Prescription.issue(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", ITEMS);

        rx.expire();

        assertThat(rx.getStatus()).isEqualTo(PrescriptionStatus.ISSUED);
    }

    @Test
    void expire_on_non_issued_prescription_throws() {
        var rx = Prescription.issue(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", ITEMS)
                .fulfill(PHARMACIST);

        assertThatThrownBy(rx::expire)
                .isInstanceOf(InvalidPrescriptionStateException.class);
    }

    @Test
    void isExpirable_returns_true_only_for_issued_status() {
        var issued = Prescription.issue(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", ITEMS);
        var fulfilled = issued.fulfill(PHARMACIST);
        var cancelled = Prescription.issue(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", ITEMS)
                .cancel(DOCTOR, "reason");

        assertThat(issued.isExpirable()).isTrue();
        assertThat(fulfilled.isExpirable()).isFalse();
        assertThat(cancelled.isExpirable()).isFalse();
    }
}
