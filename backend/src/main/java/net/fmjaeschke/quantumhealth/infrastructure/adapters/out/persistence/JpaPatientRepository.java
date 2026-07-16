package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import jakarta.data.page.PageRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.fmjaeschke.quantumhealth.application.ports.out.AppointmentRepository;
import net.fmjaeschke.quantumhealth.application.ports.out.PatientRepository;
import net.fmjaeschke.quantumhealth.domain.model.Patient;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.PatientPage;
import net.fmjaeschke.quantumhealth.domain.model.PatientQuery;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import org.hibernate.query.Order;
import org.hibernate.query.restriction.Restriction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JpaPatientRepository implements PatientRepository {

    @Inject
    JpaPatientDataRepository dataRepository;

    private final AppointmentRepository appointmentRepository;

    public JpaPatientRepository(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    public PatientId save(Patient patient) {
        dataRepository.insert(JpaPatient.from(patient));
        return patient.getId();
    }

    @Override
    public Optional<Patient> findById(PatientId id) {
        return dataRepository.findById(id.value()).map(JpaPatient::toDomain);
    }

    @Override
    public PatientPage findAll(PatientQuery query) {
        return executeQuery(null, query);
    }

    @Override
    public PatientPage findByDoctor(UserId doctorId, PatientQuery query) {
        var ids = appointmentRepository.getPatientIdsByDoctor(doctorId)
                .stream()
                .map(PatientId::value)
                .toList();
        if (ids.isEmpty()) {
            return new PatientPage(List.of(), 0, query.page(), query.size());
        }
        return executeQuery(ids, query);
    }

    private PatientPage executeQuery(List<UUID> doctorPatientIds, PatientQuery query) {
        // TODO: Revisit when Jakarta Data 1.1 is supported by Quarkus.
        // This can likely be replaced with Jakarta Data's standard Restrict/Order API
        // instead of Hibernate-specific Restriction/Order types.
        Restriction<JpaPatient> restriction = Restriction.unrestricted();

        if (doctorPatientIds != null) {
            restriction = restriction.and(Restriction.in(JpaPatient_.id, doctorPatientIds));
        }
        if (query.search().isPresent()) {
            var s = query.search().get();
            restriction = restriction.and(
                    Restriction.contains(JpaPatient_.firstName, s, false)
                            .or(Restriction.contains(JpaPatient_.lastName, s, false)));
        }
        if (query.dateOfBirth().isPresent()) {
            restriction = restriction.and(Restriction.equal(JpaPatient_.dateOfBirth, query.dateOfBirth().get()));
        }

        var order = switch (query.sortField()) {
            case FIRST_NAME -> query.sortDirection() == PatientQuery.SortDirection.DESC
                    ? Order.desc(JpaPatient_.firstName)
                    : Order.asc(JpaPatient_.firstName);
            case LAST_NAME -> query.sortDirection() == PatientQuery.SortDirection.DESC
                    ? Order.desc(JpaPatient_.lastName)
                    : Order.asc(JpaPatient_.lastName);
            case DATE_OF_BIRTH -> query.sortDirection() == PatientQuery.SortDirection.DESC
                    ? Order.desc(JpaPatient_.dateOfBirth)
                    : Order.asc(JpaPatient_.dateOfBirth);
        };

        // Jakarta Data's PageRequest is 1-indexed; the domain/port convention is 0-indexed.
        // The conversion happens only here, at the adapter boundary - the returned
        // PatientPage below still reports the original 0-indexed page.
        var pageRequest = PageRequest.ofPage(query.page() + 1).size(query.size());
        var result = dataRepository.matching(restriction, order, pageRequest);

        var patients = result.stream().map(JpaPatient::toDomain).toList();
        return new PatientPage(patients, result.totalElements(), query.page(), query.size());
    }
}
