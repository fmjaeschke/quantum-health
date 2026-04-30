package net.fmjaeschke.quantumhealth.application.usecase;

import net.fmjaeschke.quantumhealth.application.exception.AppointmentNotFoundException;
import net.fmjaeschke.quantumhealth.application.ports.out.AppointmentRepository;
import net.fmjaeschke.quantumhealth.domain.model.Appointment;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentId;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentStatus;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentServiceTest {

    static final UserId ACTOR = UserId.of("clerk-1");
    static final UserId DOCTOR = UserId.of("dr-smith");
    static final PatientId PATIENT = PatientId.generate();
    static final LocalDateTime TOMORROW = LocalDateTime.now().plusDays(1);

    @Test
    void schedule_saves_and_returns_scheduled_appointment() {
        var repo = new FakeRepo();
        var service = new AppointmentService(repo);

        var result = service.schedule(ACTOR, PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW);

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(result.getPatientName()).isEqualTo("Alice Smith");
        assertThat(result.getDoctorName()).isEqualTo("Dr. Smith");
        assertThat(repo.saved).hasSize(1);
    }

    @Test
    void findById_returns_appointment_when_found() {
        var appointment = Appointment.schedule(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW);
        var repo = new FakeRepo(appointment);
        var service = new AppointmentService(repo);

        var found = service.findById(appointment.getId(), ACTOR);

        assertThat(found.getId()).isEqualTo(appointment.getId());
    }

    @Test
    void findById_throws_when_not_found() {
        var service = new AppointmentService(new FakeRepo());

        AppointmentId appointmentId = AppointmentId.generate();
        assertThatThrownBy(() -> service.findById(appointmentId, ACTOR))
                .isInstanceOf(AppointmentNotFoundException.class);
    }

    @Test
    void confirm_saves_confirmed_appointment() {
        var appointment = Appointment.schedule(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW);
        var repo = new FakeRepo(appointment);
        var service = new AppointmentService(repo);

        var result = service.confirm(appointment.getId(), ACTOR);

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(repo.saved).hasSize(1);
    }

    @Test
    void cancel_saves_cancelled_appointment() {
        var appointment = Appointment.schedule(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW);
        var repo = new FakeRepo(appointment);
        var service = new AppointmentService(repo);

        var result = service.cancel(appointment.getId(), ACTOR);

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    void listByDoctor_returns_doctor_appointments() {
        var a1 = Appointment.schedule(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW);
        var a2 = Appointment.schedule(PATIENT, "Bob Jones", UserId.of("dr-other"), "Dr. Other", TOMORROW);
        var repo = new FakeRepo(a1, a2);
        var service = new AppointmentService(repo);

        var result = service.listByDoctor(DOCTOR, ACTOR);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getDoctorId()).isEqualTo(DOCTOR);
    }

    @Test
    void findAll_returns_all_appointments() {
        var a1 = Appointment.schedule(PATIENT, "Alice Smith", DOCTOR, "Dr. Smith", TOMORROW);
        var a2 = Appointment.schedule(PATIENT, "Bob Jones", UserId.of("dr-other"), "Dr. Other", TOMORROW);
        var repo = new FakeRepo(a1, a2);
        var service = new AppointmentService(repo);

        assertThat(service.findAll(ACTOR)).hasSize(2);
    }

    // --- fake ---

    static class FakeRepo implements AppointmentRepository {
        final List<Appointment> store;
        final List<Appointment> saved = new ArrayList<>();

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
        public List<Appointment> findByDoctorId(UserId doctorId, UserId actor) {
            return store.stream().filter(a -> a.getDoctorId().equals(doctorId)).toList();
        }

        @Override
        public List<Appointment> findAll(UserId actor) {
            return List.copyOf(store);
        }

        @Override
        public boolean existsByDoctorAndPatient(UserId d, PatientId p) { return false; }

        @Override
        public Set<PatientId> getPatientIdsByDoctor(UserId d) { return Set.of(); }
    }
}
