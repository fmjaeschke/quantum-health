package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import com.github.database.rider.cdi.api.DBRider;
import com.github.database.rider.core.api.dataset.DataSet;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import net.fmjaeschke.quantumhealth.application.ports.out.AppointmentRepository;
import net.fmjaeschke.quantumhealth.domain.model.Appointment;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentId;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentStatus;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@DBRider
class JpaAppointmentRepositoryTest {

    @Inject
    AppointmentRepository repository;

    private static final UUID ALICE_UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID APPT_1_UUID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID APPT_2_UUID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002");

    @Test
    @DataSet("datasets/appointment.yml")
    @Transactional
    void save_persists_new_appointment() {
        var patientId = PatientId.of(ALICE_UUID);
        var appointment = Appointment.schedule(
                patientId, "Alice Smith",
                UserId.of("doctor-1"), "Dr. One",
                LocalDateTime.of(2025, 7, 1, 9, 0));

        var saved = repository.save(appointment);

        assertThat(saved.getId()).isEqualTo(appointment.getId());
        assertThat(repository.findById(saved.getId())).isPresent();
    }

    @Test
    @DataSet("datasets/appointment.yml")
    void findById_returns_seeded_appointment() {
        var id = AppointmentId.of(APPT_1_UUID);
        var found = repository.findById(id);

        assertThat(found).isPresent();
        assertThat(found.get().getDoctorId()).isEqualTo(UserId.of("doctor-1"));
        assertThat(found.get().getPatientName()).isEqualTo("Alice Smith");
        assertThat(found.get().getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
    }

    @Test
    @DataSet("datasets/appointment.yml")
    void findById_returns_empty_for_unknown_id() {
        var found = repository.findById(AppointmentId.generate());
        assertThat(found).isEmpty();
    }

    @Test
    @DataSet("datasets/appointment.yml")
    void findAll_returns_all_seeded_appointments() {
        var actor = UserId.of("doctor-1");
        var results = repository.findAll(actor);

        assertThat(results)
                .extracting(Appointment::getId)
                .containsExactlyInAnyOrder(
                        AppointmentId.of(APPT_1_UUID),
                        AppointmentId.of(APPT_2_UUID));
    }

    @Test
    @DataSet("datasets/appointment.yml")
    void findByDoctorId_filters_by_doctor() {
        var doctorId = UserId.of("doctor-1");
        var results = repository.findByDoctorId(doctorId, doctorId);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getId()).isEqualTo(AppointmentId.of(APPT_1_UUID));
    }

    @Test
    @DataSet("datasets/appointment.yml")
    void existsByDoctorAndPatient_returns_true() {
        var exists = repository.existsByDoctorAndPatient(
                UserId.of("doctor-1"), PatientId.of(ALICE_UUID));
        assertThat(exists).isTrue();
    }

    @Test
    @DataSet("datasets/appointment.yml")
    void existsByDoctorAndPatient_returns_false() {
        var exists = repository.existsByDoctorAndPatient(
                UserId.of("doctor-unknown"), PatientId.of(ALICE_UUID));
        assertThat(exists).isFalse();
    }

    @Test
    @DataSet("datasets/appointment.yml")
    void getPatientIdsByDoctor_returns_patient_ids() {
        var patientIds = repository.getPatientIdsByDoctor(UserId.of("doctor-1"));

        assertThat(patientIds).containsExactlyInAnyOrder(PatientId.of(ALICE_UUID));
    }
}
