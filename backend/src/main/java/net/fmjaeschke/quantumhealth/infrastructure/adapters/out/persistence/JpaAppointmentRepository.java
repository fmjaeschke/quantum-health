package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceException;
import net.fmjaeschke.quantumhealth.application.exception.DuplicateAppointmentException;
import net.fmjaeschke.quantumhealth.application.ports.out.AppointmentRepository;
import net.fmjaeschke.quantumhealth.domain.model.Appointment;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentId;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentPage;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentQuery;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentStatus;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class JpaAppointmentRepository
        implements AppointmentRepository, PanacheRepositoryBase<JpaAppointment, UUID> {

    @Override
    public Appointment save(Appointment appointment) {
        var entityManager = getEntityManager();
        return entityManager.merge(JpaAppointment.from(appointment)).toDomain();
    }

    @Override
    public Appointment saveNew(Appointment appointment) {
        var entity = JpaAppointment.from(appointment);
        try {
            persistAndFlush(entity);
            return entity.toDomain();
        } catch (PersistenceException e) {
            if (e.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
                throw new DuplicateAppointmentException(appointment.getDoctorId(), appointment.getPatientId());
            }
            throw e;
        }
    }

    @Override
    public Optional<Appointment> findById(AppointmentId id) {
        return findByIdOptional(id.value()).map(JpaAppointment::toDomain);
    }

    @Override
    public AppointmentPage findAll(AppointmentQuery query) {
        var jpql = new StringBuilder("FROM JpaAppointment a WHERE 1=1");
        var params = new HashMap<String, Object>();

        query.statusFilter().ifPresent(s -> {
            jpql.append(" AND a.status = :status");
            params.put("status", s);
        });
        query.doctorIdFilter().ifPresent(id -> {
            jpql.append(" AND a.doctorId = :doctorId");
            params.put("doctorId", id.value());
        });

        var pq = find(jpql.toString(), params);
        pq.page(query.page(), query.pageSize());
        var total = pq.count();
        var appointments = pq.stream().map(JpaAppointment::toDomain).toList();
        return new AppointmentPage(appointments, total, query.page(), query.pageSize());
    }

    @Override
    public boolean existsByDoctorAndPatient(UserId doctorId, PatientId patientId) {
        return count("doctorId = ?1 and patientId = ?2",
                doctorId.value(), patientId.value()) > 0;
    }

    @Override
    public boolean existsActiveByDoctorAndPatient(UserId doctorId, PatientId patientId) {
        return count("doctorId = ?1 and patientId = ?2 and status not in (?3, ?4)",
                doctorId.value(), patientId.value(),
                AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED) > 0;
    }

    @Override
    public Set<PatientId> getPatientIdsByDoctor(UserId doctorId) {
        return list("doctorId", doctorId.value()).stream()
                .map(a -> PatientId.of(a.patientId))
                .collect(Collectors.toSet());
    }
}
