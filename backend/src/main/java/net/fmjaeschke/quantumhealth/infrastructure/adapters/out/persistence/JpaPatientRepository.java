package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import net.fmjaeschke.quantumhealth.application.ports.out.AppointmentRepository;
import net.fmjaeschke.quantumhealth.application.ports.out.PatientRepository;
import net.fmjaeschke.quantumhealth.domain.model.Patient;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.PatientPage;
import net.fmjaeschke.quantumhealth.domain.model.PatientQuery;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JpaPatientRepository implements PatientRepository, PanacheRepositoryBase<JpaPatient, UUID> {

    private final AppointmentRepository appointmentRepository;

    public JpaPatientRepository(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    public PatientId save(Patient patient) {
        persist(JpaPatient.from(patient));
        return patient.getId();
    }

    @Override
    public Optional<Patient> findById(PatientId id) {
        return findByIdOptional(id.value()).map(JpaPatient::toDomain);
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
        var jpql = new StringBuilder("FROM JpaPatient p WHERE 1=1");
        var params = new HashMap<String, Object>();

        if (doctorPatientIds != null) {
            jpql.append(" AND p.id IN :ids");
            params.put("ids", doctorPatientIds);
        }
        query.search()
                .ifPresent(s -> {
                    jpql.append(" AND (LOWER(p.firstName) LIKE :search OR LOWER(p.lastName) LIKE :search)");
                    params.put("search", "%" + s.toLowerCase() + "%");
                });
        query.dateOfBirth()
                .ifPresent(dob -> {
                    jpql.append(" AND p.dateOfBirth = :dob");
                    params.put("dob", dob);
                });
        var sort = Sort.by("p." + query.sortField().jpqlField,
                query.sortDirection() == PatientQuery.SortDirection.DESC
                        ? Sort.Direction.Descending
                        : Sort.Direction.Ascending);

        var pq = find(jpql.toString(), sort, params);
        pq.page(query.page(), query.size());
        var total = pq.count();
        var result = pq.stream()
                .map(JpaPatient::toDomain)
                .toList();
        return new PatientPage(result, total, query.page(), query.size());
    }
}
