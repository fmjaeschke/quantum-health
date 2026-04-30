package net.fmjaeschke.quantumhealth.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentTest {

    static final PatientId PATIENT_ID = PatientId.generate();
    static final UserId DOCTOR = UserId.of("dr-smith");
    static final LocalDateTime TOMORROW = LocalDateTime.now().plusDays(1);

    @Test
    void schedule_creates_appointment_with_scheduled_status() {
        var a = Appointment.schedule(PATIENT_ID, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW);

        assertThat(a.getId()).isNotNull();
        assertThat(a.getPatientId()).isEqualTo(PATIENT_ID);
        assertThat(a.getPatientName()).isEqualTo("Alice Smith");
        assertThat(a.getDoctorId()).isEqualTo(DOCTOR);
        assertThat(a.getDoctorName()).isEqualTo("Dr. Smith");
        assertThat(a.getScheduledAt()).isEqualTo(TOMORROW);
        assertThat(a.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
    }

    @Test
    void confirm_transitions_from_scheduled_to_confirmed() {
        var a = Appointment.schedule(PATIENT_ID, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW);
        var confirmed = a.confirm();

        assertThat(confirmed.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(confirmed.getId()).isEqualTo(a.getId());
    }

    @Test
    void confirm_throws_when_not_scheduled() {
        var a = Appointment.schedule(PATIENT_ID, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW);
        var confirmed = a.confirm();

        assertThatThrownBy(confirmed::confirm).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cancel_works_from_scheduled() {
        var a = Appointment.schedule(PATIENT_ID, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW);

        assertThat(a.cancel().getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    void cancel_works_from_confirmed() {
        var a = Appointment.schedule(PATIENT_ID, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW)
                .confirm();

        assertThat(a.cancel().getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    void cancel_throws_when_already_cancelled() {
        var a = Appointment.schedule(PATIENT_ID, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW)
                .cancel();

        assertThatThrownBy(a::cancel).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void isConfirmable_true_only_for_scheduled() {
        var scheduled = Appointment.schedule(PATIENT_ID, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW);
        assertThat(scheduled.isConfirmable()).isTrue();
        assertThat(scheduled.confirm().isConfirmable()).isFalse();
        assertThat(scheduled.cancel().isConfirmable()).isFalse();
    }

    @Test
    void isCancellable_true_for_scheduled_and_confirmed() {
        var scheduled = Appointment.schedule(PATIENT_ID, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW);
        assertThat(scheduled.isCancellable()).isTrue();
        assertThat(scheduled.confirm().isCancellable()).isTrue();
        assertThat(scheduled.cancel().isCancellable()).isFalse();
    }

    @Test
    void reconstitute_restores_all_fields() {
        var id = AppointmentId.generate();
        var a = Appointment.reconstitute(id, PATIENT_ID, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW, AppointmentStatus.CONFIRMED);

        assertThat(a.getId()).isEqualTo(id);
        assertThat(a.getPatientId()).isEqualTo(PATIENT_ID);
        assertThat(a.getPatientName()).isEqualTo("Alice Smith");
        assertThat(a.getDoctorId()).isEqualTo(DOCTOR);
        assertThat(a.getDoctorName()).isEqualTo("Dr. Smith");
        assertThat(a.getScheduledAt()).isEqualTo(TOMORROW);
        assertThat(a.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }
}
