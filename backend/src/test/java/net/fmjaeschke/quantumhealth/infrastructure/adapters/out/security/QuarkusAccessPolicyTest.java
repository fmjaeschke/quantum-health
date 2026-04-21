package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.security;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import net.fmjaeschke.quantumhealth.application.Permission;
import net.fmjaeschke.quantumhealth.application.exception.AccessDeniedException;
import net.fmjaeschke.quantumhealth.application.ports.out.AccessPolicy;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
class QuarkusAccessPolicyTest {

    private static final PatientId PATIENT = PatientId.generate();
    @Inject
    AccessPolicy accessPolicy;

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void clerk_may_read_patient_without_instance_check() {
        assertThatNoException().isThrownBy(() -> accessPolicy.check(Permission.READ_PATIENT, UserId.of("clerk-1"), PATIENT));
    }

    @Test
    @TestSecurity(user = "admin-1", roles = {"ADMIN"})
    void admin_may_read_patient_without_instance_check() {
        assertThatNoException().isThrownBy(() -> accessPolicy.check(Permission.READ_PATIENT, UserId.of("admin-1"), PATIENT));
    }

    @Test
    @TestSecurity(user = "nurse-1", roles = {"NURSE"})
    void nurse_may_read_patient_without_instance_check() {
        assertThatNoException().isThrownBy(() -> accessPolicy.check(Permission.READ_PATIENT, UserId.of("nurse-1"), PATIENT));
    }

    @Test
    @TestSecurity(user = "doctor-1", roles = {"DOCTOR"})
    void doctor_is_denied_when_no_appointment_exists() {
        assertThatThrownBy(() -> accessPolicy.check(Permission.READ_PATIENT, UserId.of("doctor-1"), PATIENT)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void register_patient_requires_no_instance_check() {
        assertThatNoException().isThrownBy(() -> accessPolicy.check(Permission.REGISTER_PATIENT, UserId.of("clerk-1"), PATIENT));
    }

    @Test
    @TestSecurity(user = "doctor-1", roles = {"DOCTOR"})
    void isDoctor_returns_true_for_doctor_role() {
        assertThat(accessPolicy.isDoctor(UserId.of("doctor-1"))).isTrue();
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void isDoctor_returns_false_for_non_doctor_role() {
        assertThat(accessPolicy.isDoctor(UserId.of("clerk-1"))).isFalse();
    }
}
