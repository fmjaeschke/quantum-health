package net.fmjaeschke.quantumhealth.application.ports.in;

import net.fmjaeschke.quantumhealth.domain.model.EncounterId;
import net.fmjaeschke.quantumhealth.domain.model.Invoice;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;

public interface GenerateInvoiceUseCase {
    Invoice generate(EncounterId encounterId, PatientId patientId);
}
