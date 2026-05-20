package net.fmjaeschke.quantumhealth.application.usecase;

import net.fmjaeschke.quantumhealth.application.Permission;
import net.fmjaeschke.quantumhealth.application.exception.AppointmentNotFoundException;
import net.fmjaeschke.quantumhealth.application.exception.DoctorNotFoundException;
import net.fmjaeschke.quantumhealth.application.exception.PatientNotFoundException;
import net.fmjaeschke.quantumhealth.application.ports.out.AccessPolicy;
import net.fmjaeschke.quantumhealth.application.ports.out.AppointmentRepository;
import net.fmjaeschke.quantumhealth.application.ports.out.DoctorPort;
import net.fmjaeschke.quantumhealth.application.ports.out.PatientRepository;
import net.fmjaeschke.quantumhealth.domain.model.Appointment;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentId;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentPage;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentQuery;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentStatus;
import net.fmjaeschke.quantumhealth.domain.model.Doctor;
import net.fmjaeschke.quantumhealth.domain.model.Patient;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.PatientPage;
import net.fmjaeschke.quantumhealth.domain.model.PatientQuery;
import net.fmjaeschke.quantumhealth.domain.model.ResourceId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentServiceTest {

    static final UserId ACTOR = UserId.of("clerk-1");
    static final UserId DOCTOR_ID = UserId.of("dr-smith");
    static final PatientId PATIENT = PatientId.generate();
    static final LocalDateTime TOMORROW = LocalDateTime.now().plusDays(1);
    static final String REASON = "Annual checkup";

    static final Patient ALICE = Patient.reconstitute(PATIENT, "Alice", "Smith", LocalDate.of(1990, 1, 1));
    static final Doctor DR_SMITH = new Doctor(DOCTOR_ID, "Dr. Smith");

    private static AppointmentService service(FakeRepo repo, boolean isDoctor) {
        return new AppointmentService(repo, new FakePatientRepo(ALICE), new FakeDoctorPort(DR_SMITH), new FakeAccessPolicy(isDoctor));
    }

    @Test
    void schedule_resolves_patient_and_doctor_names_then_returns_pending_appointment() {
        var repo = new FakeRepo();
        var service = service(repo, false);

        var result = service.schedule(ACTOR, PATIENT, DOCTOR_ID, TOMORROW, REASON);

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(result.getReason()).isEqualTo(REASON);
        assertThat(result.getPatientName()).isEqualTo("Alice Smith");
        assertThat(result.getDoctorName()).isEqualTo("Dr. Smith");
        assertThat(repo.saved).hasSize(1);
    }

    @Test
    void schedule_throws_PatientNotFoundException_when_patient_not_found() {
        var service = new AppointmentService(new FakeRepo(), new FakePatientRepo(), new FakeDoctorPort(DR_SMITH), new FakeAccessPolicy(false));

        assertThatThrownBy(() -> service.schedule(ACTOR, PATIENT, DOCTOR_ID, TOMORROW, REASON))
                .isInstanceOf(PatientNotFoundException.class);
    }

    @Test
    void schedule_throws_DoctorNotFoundException_when_doctor_not_found() {
        var service = new AppointmentService(new FakeRepo(), new FakePatientRepo(ALICE), new FakeDoctorPort(), new FakeAccessPolicy(false));

        assertThatThrownBy(() -> service.schedule(ACTOR, PATIENT, DOCTOR_ID, TOMORROW, REASON))
                .isInstanceOf(DoctorNotFoundException.class);
    }

    @Test
    void findDoctors_delegates_to_port() {
        var service = service(new FakeRepo(), false);

        assertThat(service.findDoctors()).containsExactly(DR_SMITH);
    }

    @Test
    void findById_returns_appointment_when_found() {
        var appointment = Appointment.schedule(PATIENT, "Alice Smith", DOCTOR_ID, "Dr. Smith", TOMORROW, REASON);
        var service = service(new FakeRepo(appointment), false);

        var found = service.findById(appointment.getId(), ACTOR);

        assertThat(found.getId()).isEqualTo(appointment.getId());
    }

    @Test
    void findById_throws_when_not_found() {
        var service = service(new FakeRepo(), false);

        AppointmentId appointmentId = AppointmentId.generate();
        assertThatThrownBy(() -> service.findById(appointmentId, ACTOR))
                .isInstanceOf(AppointmentNotFoundException.class);
    }

    @Test
    void confirm_saves_confirmed_appointment() {
        var appointment = Appointment.schedule(PATIENT, "Alice Smith", DOCTOR_ID, "Dr. Smith", TOMORROW, REASON);
        var repo = new FakeRepo(appointment);
        var service = service(repo, false);

        var result = service.confirm(appointment.getId(), ACTOR);

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(repo.saved).hasSize(1);
    }

    @Test
    void cancel_saves_cancelled_appointment() {
        var appointment = Appointment.schedule(PATIENT, "Alice Smith", DOCTOR_ID, "Dr. Smith", TOMORROW, REASON);
        var service = service(new FakeRepo(appointment), false);

        var result = service.cancel(appointment.getId(), ACTOR);

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    void checkIn_saves_arrived_appointment() {
        var appointment = Appointment.schedule(PATIENT, "Alice Smith", DOCTOR_ID, "Dr. Smith", TOMORROW, REASON).confirm();
        var repo = new FakeRepo(appointment);
        var service = service(repo, false);

        var result = service.checkIn(appointment.getId(), ACTOR);

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.ARRIVED);
        assertThat(repo.saved).hasSize(1);
    }

    @Test
    void start_saves_in_progress_appointment() {
        var appointment = Appointment.schedule(PATIENT, "Alice Smith", DOCTOR_ID, "Dr. Smith", TOMORROW, REASON).confirm();
        var service = service(new FakeRepo(appointment), false);

        var result = service.start(appointment.getId(), ACTOR);

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.IN_PROGRESS);
    }

    @Test
    void checkIn_throws_when_appointment_not_found() {
        var service = service(new FakeRepo(), false);

        AppointmentId appointmentId = AppointmentId.generate();

        assertThatThrownBy(() -> service.checkIn(appointmentId, ACTOR))
                .isInstanceOf(AppointmentNotFoundException.class);
    }

    // --- list behavior ---

    @Test
    void start_throws_when_appointment_not_found() {
        var service = service(new FakeRepo(), false);

        AppointmentId appointmentId = AppointmentId.generate();

        assertThatThrownBy(() -> service.start(appointmentId, ACTOR))
                .isInstanceOf(AppointmentNotFoundException.class);
    }

    @Test
    void list_for_clerk_with_no_filter_passes_unfiltered_query_to_repository() {
        var a1 = Appointment.schedule(PATIENT, "Alice Smith", DOCTOR_ID, "Dr. Smith", TOMORROW, REASON);
        var a2 = Appointment.schedule(PATIENT, "Bob Jones", UserId.of("dr-other"), "Dr. Other", TOMORROW, REASON);
        var repo = new FakeRepo(a1, a2);
        var service = service(repo, false);

        var query = AppointmentQuery.unfiltered(0, 20);
        var page = service.list(query, ACTOR);

        assertThat(page.appointments()).hasSize(2);
        assertThat(repo.lastQuery.doctorIdFilter()).isEmpty();
        assertThat(repo.lastQuery.statusFilter()).isEmpty();
    }

    @Test
    void list_for_doctor_forces_doctorIdFilter_regardless_of_incoming_query() {
        var a1 = Appointment.schedule(PATIENT, "Alice Smith", DOCTOR_ID, "Dr. Smith", TOMORROW, REASON);
        var repo = new FakeRepo(a1);
        var service = service(repo, true);

        var query = AppointmentQuery.unfiltered(0, 20);
        service.list(query, DOCTOR_ID);

        assertThat(repo.lastQuery.doctorIdFilter()).contains(DOCTOR_ID);
    }

    // --- helpers ---

    @Test
    void list_with_status_filter_passes_it_through_when_actor_is_not_doctor() {
        var a1 = Appointment.schedule(PATIENT, "Alice Smith", DOCTOR_ID, "Dr. Smith", TOMORROW, REASON);
        var repo = new FakeRepo(a1);
        var service = service(repo, false);

        var query = new AppointmentQuery(Optional.of(AppointmentStatus.PENDING), Optional.empty(), 0, 20);
        service.list(query, ACTOR);

        assertThat(repo.lastQuery.statusFilter()).contains(AppointmentStatus.PENDING);
    }

    // --- fakes ---

    static class FakeRepo implements AppointmentRepository {
        final List<Appointment> store;
        final List<Appointment> saved = new ArrayList<>();
        AppointmentQuery lastQuery;

        FakeRepo(Appointment... appointments) {
            this.store = new ArrayList<>(List.of(appointments));
        }

        @Override
        public Appointment save(Appointment a) {
            saved.add(a);
            store.removeIf(existing -> existing.getId().equals(a.getId()));
            store.add(a);
            return a;
        }

        @Override
        public Optional<Appointment> findById(AppointmentId id) {
            return store.stream().filter(a -> a.getId().equals(id)).findFirst();
        }

        @Override
        public AppointmentPage findAll(AppointmentQuery query) {
            this.lastQuery = query;
            var filtered = store.stream()
                    .filter(a -> query.statusFilter().isEmpty() || a.getStatus().equals(query.statusFilter().get()))
                    .filter(a -> query.doctorIdFilter().isEmpty() || a.getDoctorId().equals(query.doctorIdFilter().get()))
                    .toList();
            return new AppointmentPage(filtered, filtered.size(), query.page(), query.pageSize());
        }

        @Override
        public boolean existsByDoctorAndPatient(UserId d, PatientId p) {
            return false;
        }

        @Override
        public Set<PatientId> getPatientIdsByDoctor(UserId d) {
            return Set.of();
        }
    }

    static class FakeAccessPolicy implements AccessPolicy {
        private final boolean isDoctor;

        FakeAccessPolicy(boolean isDoctor) {
            this.isDoctor = isDoctor;
        }

        @Override
        public void check(Permission permission, UserId actor, ResourceId resource) {
        }

        @Override
        public boolean isAllowed(Permission permission, UserId actor) {
            return true;
        }

        @Override
        public boolean isDoctor(UserId actor) {
            return isDoctor;
        }
    }

    static class FakePatientRepo implements PatientRepository {
        private final List<Patient> store;

        FakePatientRepo(Patient... patients) {
            this.store = List.of(patients);
        }

        @Override
        public PatientId save(Patient patient) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Patient> findById(PatientId id) {
            return store.stream().filter(p -> p.getId().equals(id)).findFirst();
        }

        @Override
        public PatientPage findAll(PatientQuery query) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PatientPage findByDoctor(UserId doctorId, PatientQuery query) {
            throw new UnsupportedOperationException();
        }
    }

    static class FakeDoctorPort implements DoctorPort {
        private final List<Doctor> store;

        FakeDoctorPort(Doctor... doctors) {
            this.store = List.of(doctors);
        }

        @Override
        public List<Doctor> findByRole(String role) {
            return store;
        }

        @Override
        public Optional<Doctor> findById(UserId id) {
            return store.stream().filter(d -> d.id().equals(id)).findFirst();
        }
    }
}
