package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Update;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaInvoiceDataRepository extends BasicRepository<JpaInvoice, UUID> {

    @Insert
    JpaInvoice insert(JpaInvoice entity);

    @Update
    JpaInvoice update(JpaInvoice entity);

    @Find
    Optional<JpaInvoice> findByEncounterId(UUID encounterId);
}
