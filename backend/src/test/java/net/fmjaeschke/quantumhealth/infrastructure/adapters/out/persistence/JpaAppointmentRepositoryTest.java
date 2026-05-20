package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import com.github.database.rider.cdi.api.DBRider;
import com.github.database.rider.core.api.dataset.DataSet;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import net.fmjaeschke.quantumhealth.application.ports.out.AppointmentRepository;
import net.fmjaeschke.quantumhealth.domain.model.Appointment;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentId;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentQuery;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentStatus;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@DBRider
class JpaAppointmentRepositoryTest {

    @Inject
    AppointmentRepository repository;

    private static final UUID ALICE_UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID APPT_1_UUID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");

    @Test
    @DataSet("datasets/appointment.yml")
    @Transactional
    void save_persists_new_appointment_with_reason() {
        var patientId = PatientId.of(ALICE_UUID);
        var appointment = Appointment.schedule(
                patientId, "Alice Smith",
                UserId.of("doctor-1"), "Dr. One",
                LocalDateTime.of(2025, 7, 1, 9, 0),
                "Routine screening");

        var saved = repository.save(appointment);

        assertThat(saved.getId()).isEqualTo(appointment.getId());
        assertThat(saved.getReason()).isEqualTo("Routine screening");
        var retrieved = repository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getReason()).isEqualTo("Routine screening");
    }

    @Test
    @DataSet("datasets/appointment.yml")
    void findById_returns_seeded_appointment_with_reason() {
        var id = AppointmentId.of(APPT_1_UUID);
        var found = repository.findById(id);

        assertThat(found).isPresent();
        assertThat(found.get().getDoctorId()).isEqualTo(UserId.of("doctor-1"));
        assertThat(found.get().getPatientName()).isEqualTo("Alice Smith");
        assertThat(found.get().getReason()).isEqualTo("Annual checkup");
        assertThat(found.get().getStatus()).isEqualTo(AppointmentStatus.PENDING);
    }

    @Test
    @DataSet("datasets/appointment.yml")
    void findById_returns_empty_for_unknown_id() {
        var found = repository.findById(AppointmentId.generate());
        assertThat(found).isEmpty();
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

    // --- findAll(AppointmentQuery) ---

    @Test
    @DataSet("datasets/appointment-filter.yml")
    void findAll_without_filters_returns_all_seeded_appointments() {
        var query = AppointmentQuery.unfiltered(0, 20);
        var page = repository.findAll(query);

        assertThat(page.appointments()).hasSize(6);
        assertThat(page.totalElements()).isEqualTo(6);
    }

    @Test
    @DataSet("datasets/appointment-filter.yml")
    void findAll_with_status_filter_returns_only_matching_appointments() {
        var query = new AppointmentQuery(Optional.of(AppointmentStatus.PENDING), Optional.empty(), 0, 20);
        var page = repository.findAll(query);

        assertThat(page.appointments()).hasSize(2);
        assertThat(page.appointments()).allMatch(a -> a.getStatus() == AppointmentStatus.PENDING);
    }

    @Test
    @DataSet("datasets/appointment-filter.yml")
    void findAll_with_doctorId_filter_returns_only_that_doctors_appointments() {
        var doctorId = UserId.of("doctor-1");
        var query = new AppointmentQuery(Optional.empty(), Optional.of(doctorId), 0, 20);
        var page = repository.findAll(query);

        assertThat(page.appointments()).hasSize(3);
        assertThat(page.appointments()).allMatch(a -> a.getDoctorId().equals(doctorId));
    }

    @Test
    @DataSet("datasets/appointment-filter.yml")
    void findAll_with_status_and_doctorId_filter_applies_both() {
        var doctorId = UserId.of("doctor-1");
        var query = new AppointmentQuery(Optional.of(AppointmentStatus.PENDING), Optional.of(doctorId), 0, 20);
        var page = repository.findAll(query);

        assertThat(page.appointments()).hasSize(2);
        assertThat(page.appointments()).allMatch(a ->
                a.getStatus() == AppointmentStatus.PENDING && a.getDoctorId().equals(doctorId));
    }

    @Test
    @DataSet("datasets/appointment-filter.yml")
    void findAll_page_0_size_3_returns_first_three() {
        var query = AppointmentQuery.unfiltered(0, 3);
        var page = repository.findAll(query);

        assertThat(page.appointments()).hasSize(3);
        assertThat(page.totalElements()).isEqualTo(6);
        assertThat(page.page()).isZero();
        assertThat(page.pageSize()).isEqualTo(3);
    }

    @Test
    @DataSet("datasets/appointment-filter.yml")
    void findAll_page_1_size_3_returns_next_three() {
        var query = AppointmentQuery.unfiltered(1, 3);
        var page = repository.findAll(query);

        assertThat(page.appointments()).hasSize(3);
        assertThat(page.totalElements()).isEqualTo(6);
        assertThat(page.page()).isEqualTo(1);
    }

    @Test
    @DataSet("datasets/appointment-filter.yml")
    void findAll_page_0_and_page_1_return_distinct_appointments() {
        var page0 = repository.findAll(AppointmentQuery.unfiltered(0, 3));
        var page1 = repository.findAll(AppointmentQuery.unfiltered(1, 3));

        var ids0 = page0.appointments().stream().map(Appointment::getId).toList();
        var ids1 = page1.appointments().stream().map(Appointment::getId).toList();
        assertThat(ids0).isNotEmpty().doesNotContainAnyElementsOf(ids1);
    }
}
