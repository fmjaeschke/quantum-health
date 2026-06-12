package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import net.fmjaeschke.quantumhealth.application.ports.out.PrescriptionRepository;
import net.fmjaeschke.quantumhealth.domain.model.Prescription;
import net.fmjaeschke.quantumhealth.domain.model.PrescriptionId;
import net.fmjaeschke.quantumhealth.domain.model.PrescriptionPage;
import net.fmjaeschke.quantumhealth.domain.model.PrescriptionStatus;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JpaPrescriptionRepository
        implements PrescriptionRepository, PanacheRepositoryBase<JpaPrescription, UUID> {

    @Override
    public Prescription saveNew(Prescription prescription) {
        var entity = JpaPrescription.from(prescription);
        persistAndFlush(entity);
        return entity.toDomain();
    }

    @Override
    public Prescription save(Prescription prescription) {
        var merged = getEntityManager().merge(JpaPrescription.from(prescription));
        getEntityManager().flush();
        return merged.toDomain();
    }

    @Override
    public Optional<Prescription> findById(PrescriptionId id) {
        return findByIdOptional(id.value()).map(JpaPrescription::toDomain);
    }

    @Override
    public PrescriptionPage findAll(int page, int pageSize, Optional<UserId> doctorId) {
        var pq = doctorId.isPresent()
                ? find("FROM JpaPrescription p WHERE 1=1 AND p.doctorId = ?1", doctorId.get().value())
                : find("FROM JpaPrescription p WHERE 1=1");
        pq.page(page, pageSize);
        var total = pq.count();
        var prescriptions = pq.stream().map(JpaPrescription::toDomain).toList();
        return new PrescriptionPage(prescriptions, total, page, pageSize);
    }

    @Override
    public List<Prescription> findStale(Instant threshold) {
        return find("status = ?1 AND issuedAt < ?2", PrescriptionStatus.ISSUED, threshold)
                .stream().map(JpaPrescription::toDomain).toList();
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void expireOne(Prescription prescription) {
        save(prescription.expire());
    }
}
