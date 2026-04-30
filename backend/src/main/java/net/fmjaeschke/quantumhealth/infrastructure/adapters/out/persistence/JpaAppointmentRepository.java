package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import net.fmjaeschke.quantumhealth.application.ports.out.AppointmentRepository;
import net.fmjaeschke.quantumhealth.domain.model.Appointment;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentId;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class JpaAppointmentRepository
        implements AppointmentRepository, PanacheRepositoryBase<JpaAppointment, UUID> {

    @Override
    public Appointment save(Appointment appointment) {
        var managed = getEntityManager().merge(JpaAppointment.from(appointment));
        return managed.toDomain();
    }

    @Override
    public Optional<Appointment> findById(AppointmentId id) {
        return findByIdOptional(id.value()).map(JpaAppointment::toDomain);
    }

    @Override
    public List<Appointment> findByDoctorId(UserId doctorId, UserId actor) {
        return list("doctorId", doctorId.value())
                .stream().map(JpaAppointment::toDomain).toList();
    }

    @Override
    public List<Appointment> findAll(UserId actor) {
        return this.listAll().stream().map(JpaAppointment::toDomain).toList();
    }

    @Override
    public boolean existsByDoctorAndPatient(UserId doctorId, PatientId patientId) {
        return count("doctorId = ?1 and patientId = ?2",
                doctorId.value(), patientId.value()) > 0;
    }

    @Override
    public Set<PatientId> getPatientIdsByDoctor(UserId doctorId) {
        return list("doctorId", doctorId.value()).stream()
                .map(a -> PatientId.of(a.patientId))
                .collect(Collectors.toSet());
    }
}
