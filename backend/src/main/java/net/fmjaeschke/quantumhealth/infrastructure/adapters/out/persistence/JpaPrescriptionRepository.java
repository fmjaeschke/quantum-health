package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import jakarta.data.exceptions.OptimisticLockingFailureException;
import jakarta.data.page.PageRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import net.fmjaeschke.quantumhealth.application.exception.ConcurrentModificationException;
import net.fmjaeschke.quantumhealth.application.ports.out.PrescriptionRepository;
import net.fmjaeschke.quantumhealth.domain.model.Prescription;
import net.fmjaeschke.quantumhealth.domain.model.PrescriptionId;
import net.fmjaeschke.quantumhealth.domain.model.PrescriptionPage;
import net.fmjaeschke.quantumhealth.domain.model.PrescriptionStatus;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import org.hibernate.query.restriction.Restriction;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class JpaPrescriptionRepository implements PrescriptionRepository {

    @Inject
    JpaPrescriptionDataRepository dataRepository;

    @Override
    public Prescription saveNew(Prescription prescription) {
        return dataRepository.insert(JpaPrescription.from(prescription)).toDomain();
    }

    @Override
    public Prescription save(Prescription prescription) {
        try {
            // Upsert (inherited BasicRepository.save(), not @Update): mirrors
            // JpaAppointmentRepository.save() - the same merge-like behavior the previous
            // entityManager.merge() call provided, including optimistic-lock conflict
            // detection on the @Version field.
            return dataRepository.save(JpaPrescription.from(prescription)).toDomain();
        } catch (OptimisticLockingFailureException e) {
            // The generated save() wraps a stale @Version conflict (StaleStateException) as
            // OptimisticLockingFailureException; translate to the application-level exception so
            // callers (PrescriptionService, ConcurrentModificationExceptionMapper) stay free of
            // JPA/Jakarta Data types.
            throw new ConcurrentModificationException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public Optional<Prescription> findById(PrescriptionId id) {
        return dataRepository.findById(id.value()).map(JpaPrescription::toDomain);
    }

    @Override
    public PrescriptionPage findAll(int page, int pageSize, Optional<UserId> doctorId) {
        Restriction<JpaPrescription> restriction = Restriction.unrestricted();
        if (doctorId.isPresent()) {
            restriction = restriction.and(Restriction.equal(JpaPrescription_.doctorId, doctorId.get().value()));
        }

        // Jakarta Data's PageRequest is 1-indexed; the domain/port convention is 0-indexed.
        // The conversion happens only here, at the adapter boundary - the returned
        // PrescriptionPage below still reports the original 0-indexed page.
        var pageRequest = PageRequest.ofPage(page + 1).size(pageSize);
        var result = dataRepository.matching(restriction, pageRequest);

        var prescriptions = result.stream().map(JpaPrescription::toDomain).toList();
        return new PrescriptionPage(prescriptions, result.totalElements(), page, pageSize);
    }

    @Override
    public List<Prescription> findStale(Instant threshold) {
        return dataRepository.findStale(PrescriptionStatus.ISSUED, threshold)
                .stream().map(JpaPrescription::toDomain).toList();
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void expireOne(Prescription prescription) {
        save(prescription.expire());
    }
}
