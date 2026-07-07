package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.security;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import net.fmjaeschke.quantumhealth.domain.model.Permission;
import net.fmjaeschke.quantumhealth.application.exception.AccessDeniedException;
import net.fmjaeschke.quantumhealth.application.ports.out.AccessPolicy;
import net.fmjaeschke.quantumhealth.application.ports.out.AppointmentRepository;
import net.fmjaeschke.quantumhealth.application.ports.out.PrescriptionRepository;
import net.fmjaeschke.quantumhealth.domain.model.Appointment;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentId;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentStatus;
import net.fmjaeschke.quantumhealth.domain.model.Disposition;
import net.fmjaeschke.quantumhealth.domain.model.MedicationItem;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.Prescription;
import net.fmjaeschke.quantumhealth.domain.model.PrescriptionId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@QuarkusTest
class QuarkusAccessPolicyTest {

    private static final PatientId PATIENT = PatientId.generate();

    @Inject
    AccessPolicy accessPolicy;

    @InjectMock
    PrescriptionRepository prescriptionRepository;

    @InjectMock
    AppointmentRepository appointmentRepository;

    @ParameterizedTest
    @EnumSource(value = Permission.class, names = {
            "REGISTER_PATIENT", "SCHEDULE_APPOINTMENT", "WRITE_ENCOUNTER",
            "READ_ENCOUNTER", "PROCESS_BILLING"
    })
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void unwired_permissions_fail_closed_with_illegal_state_exception(Permission permission) {
        assertThatThrownBy(() -> accessPolicy.check(permission, UserId.of("clerk-1"), PATIENT))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void non_doctor_may_access_any_patient_without_treatment_history() {
        assertThat(accessPolicy.mayAccessPatient(UserId.of("clerk-1"), PATIENT)).isTrue();
    }

    @Test
    @TestSecurity(user = "admin-1", roles = {"ADMIN"})
    void admin_may_access_any_patient_without_treatment_history() {
        assertThat(accessPolicy.mayAccessPatient(UserId.of("admin-1"), PATIENT)).isTrue();
    }

    @Test
    @TestSecurity(user = "nurse-1", roles = {"NURSE"})
    void nurse_may_access_any_patient_without_treatment_history() {
        assertThat(accessPolicy.mayAccessPatient(UserId.of("nurse-1"), PATIENT)).isTrue();
    }

    @Test
    @TestSecurity(user = "doctor-1", roles = {"DOCTOR"})
    void doctor_may_access_patient_they_have_treated() {
        when(appointmentRepository.existsByDoctorAndPatient(UserId.of("doctor-1"), PATIENT)).thenReturn(true);

        assertThat(accessPolicy.mayAccessPatient(UserId.of("doctor-1"), PATIENT)).isTrue();
    }

    @Test
    @TestSecurity(user = "doctor-1", roles = {"DOCTOR"})
    void doctor_may_not_access_patient_never_treated() {
        when(appointmentRepository.existsByDoctorAndPatient(UserId.of("doctor-1"), PATIENT)).thenReturn(false);

        assertThat(accessPolicy.mayAccessPatient(UserId.of("doctor-1"), PATIENT)).isFalse();
    }

    @Test
    @TestSecurity(user = "doctor-1", roles = {"DOCTOR"})
    void doctor_may_access_only_own_resources() {
        assertThat(accessPolicy.mayAccessOwnedBy(UserId.of("doctor-1"), UserId.of("doctor-1"))).isTrue();
        assertThat(accessPolicy.mayAccessOwnedBy(UserId.of("doctor-2"), UserId.of("doctor-1"))).isFalse();
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void non_doctor_may_access_any_resource() {
        assertThat(accessPolicy.mayAccessOwnedBy(UserId.of("doctor-2"), UserId.of("clerk-1"))).isTrue();
    }

    @Test
    @TestSecurity(user = "admin-1", roles = {"ADMIN"})
    void admin_may_access_any_resource() {
        assertThat(accessPolicy.mayAccessOwnedBy(UserId.of("doctor-2"), UserId.of("admin-1"))).isTrue();
    }

    @Test
    @TestSecurity(user = "nurse-1", roles = {"NURSE"})
    void nurse_may_access_any_resource() {
        assertThat(accessPolicy.mayAccessOwnedBy(UserId.of("doctor-2"), UserId.of("nurse-1"))).isTrue();
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void clerk_may_confirm_appointment_without_instance_check() {
        var apptId = AppointmentId.generate();
        assertThatNoException().isThrownBy(
                () -> accessPolicy.check(Permission.CONFIRM_APPOINTMENT, UserId.of("clerk-1"), apptId));
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void clerk_may_check_in_patient_without_instance_check() {
        var apptId = AppointmentId.generate();
        assertThatNoException().isThrownBy(
                () -> accessPolicy.check(Permission.CHECK_IN_PATIENT, UserId.of("clerk-1"), apptId));
    }

    @Test
    @TestSecurity(user = "doctor-1", roles = {"DOCTOR"})
    void cancel_prescription_with_mismatched_resource_type_is_denied_not_a_class_cast_exception() {
        var appointmentId = AppointmentId.generate();
        assertThatThrownBy(() -> accessPolicy.check(Permission.CANCEL_PRESCRIPTION, UserId.of("doctor-1"), appointmentId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @TestSecurity(user = "doctor-1", roles = {"DOCTOR"})
    void isDoctor_returns_true_for_doctor_role() {
        assertThat(accessPolicy.isDoctor()).isTrue();
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void isDoctor_returns_false_for_non_doctor_role() {
        assertThat(accessPolicy.isDoctor()).isFalse();
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void clerk_is_allowed_to_confirm_and_check_in_and_cancel() {
        assertThat(accessPolicy.isAllowed(Permission.CONFIRM_APPOINTMENT, UserId.of("clerk-1"))).isTrue();
        assertThat(accessPolicy.isAllowed(Permission.CHECK_IN_PATIENT, UserId.of("clerk-1"))).isTrue();
        assertThat(accessPolicy.isAllowed(Permission.CANCEL_APPOINTMENT, UserId.of("clerk-1"))).isTrue();
        assertThat(accessPolicy.isAllowed(Permission.START_ENCOUNTER, UserId.of("clerk-1"))).isFalse();
    }

    @Test
    @TestSecurity(user = "doctor-1", roles = {"DOCTOR"})
    void doctor_is_allowed_to_start_and_cancel_but_not_confirm_or_check_in() {
        assertThat(accessPolicy.isAllowed(Permission.START_ENCOUNTER, UserId.of("doctor-1"))).isTrue();
        assertThat(accessPolicy.isAllowed(Permission.CANCEL_APPOINTMENT, UserId.of("doctor-1"))).isTrue();
        assertThat(accessPolicy.isAllowed(Permission.CONFIRM_APPOINTMENT, UserId.of("doctor-1"))).isFalse();
        assertThat(accessPolicy.isAllowed(Permission.CHECK_IN_PATIENT, UserId.of("doctor-1"))).isFalse();
    }

    @Test
    @TestSecurity(user = "nurse-1", roles = {"NURSE"})
    void nurse_is_not_allowed_any_appointment_transitions() {
        assertThat(accessPolicy.isAllowed(Permission.CONFIRM_APPOINTMENT, UserId.of("nurse-1"))).isFalse();
        assertThat(accessPolicy.isAllowed(Permission.CHECK_IN_PATIENT, UserId.of("nurse-1"))).isFalse();
        assertThat(accessPolicy.isAllowed(Permission.START_ENCOUNTER, UserId.of("nurse-1"))).isFalse();
        assertThat(accessPolicy.isAllowed(Permission.CANCEL_APPOINTMENT, UserId.of("nurse-1"))).isFalse();
    }

    @Test
    @TestSecurity(user = "doctor-1", roles = {"DOCTOR"})
    void doctor_can_cancel_own_prescription() {
        var rxId = PrescriptionId.generate();
        var prescription = Prescription.reconstitute(rxId, PatientId.generate(), "Alice",
                UserId.of("doctor-1"), "Dr. One",
                List.of(new MedicationItem("Aspirin", "100mg", "once daily")),
                Instant.now(), Disposition.issued(), 0L);
        when(prescriptionRepository.findById(rxId)).thenReturn(Optional.of(prescription));

        assertThatNoException().isThrownBy(
                () -> accessPolicy.check(Permission.CANCEL_PRESCRIPTION, UserId.of("doctor-1"), rxId));
    }

    @Test
    @TestSecurity(user = "doctor-1", roles = {"DOCTOR"})
    void doctor_cannot_cancel_colleague_prescription() {
        var rxId = PrescriptionId.generate();
        var prescription = Prescription.reconstitute(rxId, PatientId.generate(), "Alice",
                UserId.of("doctor-2"), "Dr. Two",
                List.of(new MedicationItem("Aspirin", "100mg", "once daily")),
                Instant.now(), Disposition.issued(), 0L);
        when(prescriptionRepository.findById(rxId)).thenReturn(Optional.of(prescription));

        assertThatThrownBy(
                () -> accessPolicy.check(Permission.CANCEL_PRESCRIPTION, UserId.of("doctor-1"), rxId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @TestSecurity(user = "admin-1", roles = {"ADMIN"})
    void admin_can_cancel_any_prescription() {
        var rxId = PrescriptionId.generate();
        var prescription = Prescription.reconstitute(rxId, PatientId.generate(), "Alice",
                UserId.of("doctor-2"), "Dr. Two",
                List.of(new MedicationItem("Aspirin", "100mg", "once daily")),
                Instant.now(), Disposition.issued(), 0L);
        when(prescriptionRepository.findById(rxId)).thenReturn(Optional.of(prescription));

        assertThatNoException().isThrownBy(
                () -> accessPolicy.check(Permission.CANCEL_PRESCRIPTION, UserId.of("admin-1"), rxId));
    }

    @Test
    @TestSecurity(user = "doctor-1", roles = {"DOCTOR"})
    void doctor_can_cancel_own_appointment() {
        var apptId = AppointmentId.generate();
        var appointment = Appointment.reconstitute(apptId, PatientId.generate(), "Alice",
                UserId.of("doctor-1"), "Dr. One", Instant.now(), "checkup",
                AppointmentStatus.PENDING);
        when(appointmentRepository.findById(apptId)).thenReturn(Optional.of(appointment));

        assertThatNoException().isThrownBy(
                () -> accessPolicy.check(Permission.CANCEL_APPOINTMENT, UserId.of("doctor-1"), apptId));
    }

    @Test
    @TestSecurity(user = "doctor-1", roles = {"DOCTOR"})
    void doctor_cannot_cancel_colleague_appointment() {
        var apptId = AppointmentId.generate();
        var appointment = Appointment.reconstitute(apptId, PatientId.generate(), "Alice",
                UserId.of("doctor-2"), "Dr. Two", Instant.now(), "checkup",
                AppointmentStatus.PENDING);
        when(appointmentRepository.findById(apptId)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(
                () -> accessPolicy.check(Permission.CANCEL_APPOINTMENT, UserId.of("doctor-1"), apptId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void clerk_may_cancel_any_appointment_without_instance_check() {
        var apptId = AppointmentId.generate();
        var appointment = Appointment.reconstitute(apptId, PatientId.generate(), "Alice",
                UserId.of("doctor-2"), "Dr. Two", Instant.now(), "checkup",
                AppointmentStatus.PENDING);
        when(appointmentRepository.findById(apptId)).thenReturn(Optional.of(appointment));

        assertThatNoException().isThrownBy(
                () -> accessPolicy.check(Permission.CANCEL_APPOINTMENT, UserId.of("clerk-1"), apptId));
    }

    @Test
    @TestSecurity(user = "admin-1", roles = {"ADMIN"})
    void admin_may_cancel_any_appointment_without_instance_check() {
        var apptId = AppointmentId.generate();
        var appointment = Appointment.reconstitute(apptId, PatientId.generate(), "Alice",
                UserId.of("doctor-2"), "Dr. Two", Instant.now(), "checkup",
                AppointmentStatus.PENDING);
        when(appointmentRepository.findById(apptId)).thenReturn(Optional.of(appointment));

        assertThatNoException().isThrownBy(
                () -> accessPolicy.check(Permission.CANCEL_APPOINTMENT, UserId.of("admin-1"), apptId));
    }

    @Test
    @TestSecurity(user = "doctor-1", roles = {"DOCTOR"})
    void doctor_can_start_own_appointment() {
        var apptId = AppointmentId.generate();
        var appointment = Appointment.reconstitute(apptId, PatientId.generate(), "Alice",
                UserId.of("doctor-1"), "Dr. One", Instant.now(), "checkup",
                AppointmentStatus.CONFIRMED);
        when(appointmentRepository.findById(apptId)).thenReturn(Optional.of(appointment));

        assertThatNoException().isThrownBy(
                () -> accessPolicy.check(Permission.START_ENCOUNTER, UserId.of("doctor-1"), apptId));
    }

    @Test
    @TestSecurity(user = "doctor-1", roles = {"DOCTOR"})
    void doctor_cannot_start_colleague_appointment() {
        var apptId = AppointmentId.generate();
        var appointment = Appointment.reconstitute(apptId, PatientId.generate(), "Alice",
                UserId.of("doctor-2"), "Dr. Two", Instant.now(), "checkup",
                AppointmentStatus.CONFIRMED);
        when(appointmentRepository.findById(apptId)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(
                () -> accessPolicy.check(Permission.START_ENCOUNTER, UserId.of("doctor-1"), apptId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @TestSecurity(user = "pharmacist-1", roles = {"PHARMACIST"})
    void pharmacist_can_dispense_medication_without_instance_check() {
        var rxId = PrescriptionId.generate();
        assertThatNoException().isThrownBy(
                () -> accessPolicy.check(Permission.DISPENSE_MEDICATION, UserId.of("pharmacist-1"), rxId));
    }
}
