package net.fmjaeschke.quantumhealth.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PatientTest {

    @Test
    void register_assigns_generated_id_and_preserves_fields() {
        var dob = LocalDate.of(1990, 5, 15);
        var patient = Patient.register("Alice", "Smith", dob);

        assertThat(patient.getId()).isNotNull();
        assertThat(patient.getFirstName()).isEqualTo("Alice");
        assertThat(patient.getLastName()).isEqualTo("Smith");
        assertThat(patient.getDateOfBirth()).isEqualTo(dob);
    }

    @Test
    void two_registered_patients_have_different_ids() {
        var p1 = Patient.register("Alice", "Smith", LocalDate.of(1990, 5, 15));
        var p2 = Patient.register("Bob", "Jones", LocalDate.of(1985, 3, 20));
        assertThat(p1.getId()).isNotEqualTo(p2.getId());
    }
}
