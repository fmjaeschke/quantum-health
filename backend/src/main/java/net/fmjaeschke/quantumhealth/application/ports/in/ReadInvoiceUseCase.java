package net.fmjaeschke.quantumhealth.application.ports.in;

import net.fmjaeschke.quantumhealth.domain.model.Invoice;
import net.fmjaeschke.quantumhealth.domain.model.InvoiceId;

public interface ReadInvoiceUseCase {
    Invoice findById(InvoiceId id);
}
