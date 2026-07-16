package net.fmjaeschke.quantumhealth.domain.model;

import net.fmjaeschke.quantumhealth.domain.exception.InvalidAppointmentStateException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentTest {

    static final PatientId PATIENT_ID = PatientId.generate();
    static final UserId DOCTOR = UserId.of("dr-smith");
    static final Instant TOMORROW = Instant.now().plus(1, ChronoUnit.DAYS);
    static final String REASON = "Annual checkup";

    @Test
    void schedule_creates_appointment_with_pending_status_and_reason() {
        var a = Appointment.schedule(PATIENT_ID, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW, REASON);

        assertThat(a.getId()).isNotNull();
        assertThat(a.getPatientId()).isEqualTo(PATIENT_ID);
        assertThat(a.getPatientName()).isEqualTo("Alice Smith");
        assertThat(a.getDoctorId()).isEqualTo(DOCTOR);
        assertThat(a.getDoctorName()).isEqualTo("Dr. Smith");
        assertThat(a.getScheduledAt()).isEqualTo(TOMORROW);
        assertThat(a.getReason()).isEqualTo(REASON);
        assertThat(a.getStatus()).isEqualTo(AppointmentStatus.PENDING);
    }

    @Test
    void confirm_transitions_from_pending_to_confirmed() {
        var a = Appointment.schedule(PATIENT_ID, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW, REASON);
        var confirmed = a.confirm();

        assertThat(confirmed.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(confirmed.getId()).isEqualTo(a.getId());
        assertThat(confirmed.getReason()).isEqualTo(REASON);
    }

    @Test
    void confirm_throws_when_not_pending() {
        var a = Appointment.schedule(PATIENT_ID, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW, REASON);
        var confirmed = a.confirm();

        assertThatThrownBy(confirmed::confirm).isInstanceOf(InvalidAppointmentStateException.class);
    }

    @Test
    void cancel_works_from_pending() {
        var a = Appointment.schedule(PATIENT_ID, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW, REASON);

        assertThat(a.cancel().getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    void cancel_works_from_confirmed() {
        var a = Appointment.schedule(PATIENT_ID, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW, REASON)
                .confirm();

        assertThat(a.cancel().getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    void cancel_throws_when_already_cancelled() {
        var a = Appointment.schedule(PATIENT_ID, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW, REASON)
                .cancel();

        assertThatThrownBy(a::cancel).isInstanceOf(InvalidAppointmentStateException.class);
    }

    @Test
    void isConfirmable_true_only_for_pending() {
        var pending = Appointment.schedule(PATIENT_ID, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW, REASON);
        assertThat(pending.isConfirmable()).isTrue();
        assertThat(pending.confirm().isConfirmable()).isFalse();
        assertThat(pending.cancel().isConfirmable()).isFalse();
    }

    @Test
    void isCancellable_true_for_pending_confirmed_arrived_and_in_progress_false_otherwise() {
        var pending    = Appointment.schedule(PATIENT_ID, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW, REASON);
        var confirmed  = pending.confirm();
        var arrived    = confirmed.checkIn();
        var inProgress = confirmed.start();
        var cancelled  = pending.cancel();

        assertThat(pending.isCancellable()).isTrue();
        assertThat(confirmed.isCancellable()).isTrue();
        assertThat(arrived.isCancellable()).isTrue();
        assertThat(inProgress.isCancellable()).isTrue();
        assertThat(cancelled.isCancellable()).isFalse();
    }

    @Test
    void cancel_works_from_in_progress() {
        var inProgress = Appointment.schedule(PATIENT_ID, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW, REASON)
                .confirm().start();
        assertThat(inProgress.cancel().getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    void confirm_throws_when_appointment_is_cancelled() {
        var cancelled = Appointment.schedule(PATIENT_ID, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW, REASON)
                .cancel();
        assertThatThrownBy(cancelled::confirm).isInstanceOf(InvalidAppointmentStateException.class);
    }

    @Test
    void checkIn_transitions_from_confirmed_to_arrived() {
        var a = Appointment.schedule(PATIENT_ID, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW, REASON).confirm();
        assertThat(a.checkIn().getStatus()).isEqualTo(AppointmentStatus.ARRIVED);
    }

    @Test
    void checkIn_throws_when_not_confirmed() {
        var pending = Appointment.schedule(PATIENT_ID, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW, REASON);
        assertThatThrownBy(pending::checkIn).isInstanceOf(InvalidAppointmentStateException.class);
    }

    @Test
    void start_transitions_from_confirmed_to_in_progress() {
        var confirmed = Appointment.schedule(PATIENT_ID, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW, REASON).confirm();
        assertThat(confirmed.start().getStatus()).isEqualTo(AppointmentStatus.IN_PROGRESS);
    }

    @Test
    void start_transitions_from_arrived_to_in_progress() {
        var arrived = Appointment.schedule(PATIENT_ID, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW, REASON)
                .confirm().checkIn();
        assertThat(arrived.start().getStatus()).isEqualTo(AppointmentStatus.IN_PROGRESS);
    }

    @Test
    void start_throws_when_pending() {
        var pending = Appointment.schedule(PATIENT_ID, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW, REASON);
        assertThatThrownBy(pending::start).isInstanceOf(InvalidAppointmentStateException.class);
    }

    @Test
    void isCheckInnable_true_only_for_confirmed() {
        var pending = Appointment.schedule(PATIENT_ID, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW, REASON);
        assertThat(pending.isCheckInnable()).isFalse();
        assertThat(pending.confirm().isCheckInnable()).isTrue();
        assertThat(pending.cancel().isCheckInnable()).isFalse();
    }

    @Test
    void isStartable_true_for_confirmed_and_arrived() {
        var pending = Appointment.schedule(PATIENT_ID, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW, REASON);
        var confirmed = pending.confirm();
        var arrived = confirmed.checkIn();
        assertThat(pending.isStartable()).isFalse();
        assertThat(confirmed.isStartable()).isTrue();
        assertThat(arrived.isStartable()).isTrue();
        assertThat(pending.cancel().isStartable()).isFalse();
    }

    @Test
    void complete_transitions_from_in_progress_to_completed() {
        var inProgress = Appointment.schedule(PATIENT_ID, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW, REASON)
                .confirm().start();
        assertThat(inProgress.complete().getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    void complete_throws_when_not_in_progress() {
        var confirmed = Appointment.schedule(PATIENT_ID, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW, REASON)
                .confirm();
        assertThatThrownBy(confirmed::complete).isInstanceOf(InvalidAppointmentStateException.class);
    }

    @Test
    void isCompletable_true_only_for_in_progress() {
        var confirmed = Appointment.schedule(PATIENT_ID, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW, REASON)
                .confirm();
        var inProgress = confirmed.start();
        assertThat(confirmed.isCompletable()).isFalse();
        assertThat(inProgress.isCompletable()).isTrue();
    }

    @Test
    void reconstitute_restores_all_fields_including_reason() {
        var id = AppointmentId.generate();
        var a = Appointment.reconstitute(id, PATIENT_ID, "Alice Smith", DOCTOR, "Dr. Smith",
                TOMORROW, REASON, AppointmentStatus.CONFIRMED);

        assertThat(a.getId()).isEqualTo(id);
        assertThat(a.getPatientId()).isEqualTo(PATIENT_ID);
        assertThat(a.getPatientName()).isEqualTo("Alice Smith");
        assertThat(a.getDoctorId()).isEqualTo(DOCTOR);
        assertThat(a.getDoctorName()).isEqualTo("Dr. Smith");
        assertThat(a.getScheduledAt()).isEqualTo(TOMORROW);
        assertThat(a.getReason()).isEqualTo(REASON);
        assertThat(a.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }
}
