package net.fmjaeschke.quantumhealth.application.ports.out;

import net.fmjaeschke.quantumhealth.domain.model.EncounterId;
import net.fmjaeschke.quantumhealth.domain.model.Invoice;
import net.fmjaeschke.quantumhealth.domain.model.InvoiceId;

import java.util.Optional;

public interface InvoiceRepository {
    Invoice saveNew(Invoice invoice);
    Invoice save(Invoice invoice);
    Optional<Invoice> findById(InvoiceId id);
    Optional<Invoice> findByEncounterId(EncounterId encounterId);
}
