package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import net.fmjaeschke.quantumhealth.domain.model.PrescriptionStatus;
import org.hibernate.query.restriction.Restriction;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaPrescriptionDataRepository extends BasicRepository<JpaPrescription, UUID> {

    @Insert
    JpaPrescription insert(JpaPrescription entity);

    @Find
    Page<JpaPrescription> matching(Restriction<JpaPrescription> restriction, PageRequest pageRequest);

    @Query("select p from JpaPrescription p where p.status = :status and p.issuedAt < :threshold")
    List<JpaPrescription> findStale(PrescriptionStatus status, Instant threshold);
}
