package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import jakarta.data.exceptions.EntityExistsException;
import jakarta.data.page.PageRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.query.restriction.Restriction;
import net.fmjaeschke.quantumhealth.application.exception.DuplicateAppointmentException;
import net.fmjaeschke.quantumhealth.application.ports.out.AppointmentRepository;
import net.fmjaeschke.quantumhealth.domain.model.Appointment;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentId;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentPage;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentQuery;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class JpaAppointmentRepository implements AppointmentRepository {

    private static final String UQ_ACTIVE_APPOINTMENT = "uq_active_appointment";

    @Inject
    JpaAppointmentDataRepository dataRepository;

    @Override
    public Appointment save(Appointment appointment) {
        // Upsert (inherited BasicRepository.save(), not @Update): callers may pass either an
        // already-persisted appointment (state-transition updates) or, as in
        // save_round_trips_scheduledAt_as_exact_utc_instant, a brand-new one - the same
        // merge-like behavior the previous entityManager.merge() call provided.
        return dataRepository.save(JpaAppointment.from(appointment)).toDomain();
    }

    @Override
    public Appointment saveNew(Appointment appointment) {
        var entity = JpaAppointment.from(appointment);
        try {
            return dataRepository.insert(entity).toDomain();
        } catch (EntityExistsException e) {
            // The generated @Insert method wraps any ConstraintViolationException (unique
            // or FK) as EntityExistsException, preserving the original as its cause.
            if (e.getCause() instanceof ConstraintViolationException cve
                    && UQ_ACTIVE_APPOINTMENT.equals(cve.getConstraintName())) {
                throw new DuplicateAppointmentException(appointment.getDoctorId(), appointment.getPatientId());
            }
            throw (ConstraintViolationException) e.getCause();
        }
    }

    @Override
    public Optional<Appointment> findById(AppointmentId id) {
        return dataRepository.findById(id.value()).map(JpaAppointment::toDomain);
    }

    @Override
    public AppointmentPage findAll(AppointmentQuery query) {
        Restriction<JpaAppointment> restriction = Restriction.unrestricted();
        if (query.statusFilter().isPresent()) {
            restriction = restriction.and(Restriction.equal(JpaAppointment_.status, query.statusFilter().get()));
        }
        if (query.doctorIdFilter().isPresent()) {
            restriction = restriction.and(
                    Restriction.equal(JpaAppointment_.doctorId, query.doctorIdFilter().get().value()));
        }

        // Jakarta Data's PageRequest is 1-indexed; the domain/port convention is 0-indexed.
        // The conversion happens only here, at the adapter boundary - the returned
        // AppointmentPage below still reports the original 0-indexed query.page().
        var pageRequest = PageRequest.ofPage(query.page() + 1).size(query.pageSize());
        var page = dataRepository.matching(restriction, pageRequest);

        var appointments = page.stream().map(JpaAppointment::toDomain).toList();
        return new AppointmentPage(appointments, page.totalElements(), query.page(), query.pageSize());
    }

    @Override
    public boolean existsByDoctorAndPatient(UserId doctorId, PatientId patientId) {
        return dataRepository.existsByDoctorIdAndPatientId(doctorId.value(), patientId.value());
    }

    @Override
    public Set<PatientId> getPatientIdsByDoctor(UserId doctorId) {
        return dataRepository.findByDoctorId(doctorId.value()).stream()
                .map(a -> PatientId.of(a.patientId))
                .collect(Collectors.toSet());
    }
}
