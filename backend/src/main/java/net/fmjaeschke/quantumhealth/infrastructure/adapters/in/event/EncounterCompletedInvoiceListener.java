package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.event;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import net.fmjaeschke.quantumhealth.application.ports.in.GenerateInvoiceUseCase;
import net.fmjaeschke.quantumhealth.domain.model.EncounterCompletedEvent;

@ApplicationScoped
public class EncounterCompletedInvoiceListener {

    private final GenerateInvoiceUseCase generateInvoice;

    public EncounterCompletedInvoiceListener(GenerateInvoiceUseCase generateInvoice) {
        this.generateInvoice = generateInvoice;
    }

    void onEncounterCompleted(@Observes EncounterCompletedEvent event) {
        generateInvoice.generate(event.encounterId(), event.patientId());
    }
}
