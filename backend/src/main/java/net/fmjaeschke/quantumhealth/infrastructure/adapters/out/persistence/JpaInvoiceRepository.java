package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.fmjaeschke.quantumhealth.application.ports.out.InvoiceRepository;
import net.fmjaeschke.quantumhealth.domain.model.EncounterId;
import net.fmjaeschke.quantumhealth.domain.model.Invoice;
import net.fmjaeschke.quantumhealth.domain.model.InvoiceId;

import java.util.Optional;

@ApplicationScoped
public class JpaInvoiceRepository implements InvoiceRepository {

    @Inject
    JpaInvoiceDataRepository dataRepository;

    @Override
    public Invoice saveNew(Invoice invoice) {
        return dataRepository.insert(JpaInvoice.from(invoice)).toDomain();
    }

    @Override
    public Invoice save(Invoice invoice) {
        return dataRepository.update(JpaInvoice.from(invoice)).toDomain();
    }

    @Override
    public Optional<Invoice> findById(InvoiceId id) {
        return dataRepository.findById(id.value()).map(JpaInvoice::toDomain);
    }

    @Override
    public Optional<Invoice> findByEncounterId(EncounterId encounterId) {
        return dataRepository.findByEncounterId(encounterId.value()).map(JpaInvoice::toDomain);
    }
}
