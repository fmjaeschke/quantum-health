package net.fmjaeschke.quantumhealth.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import net.fmjaeschke.quantumhealth.application.exception.InvoiceNotFoundException;
import net.fmjaeschke.quantumhealth.application.ports.in.AppealInvoiceUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.DenyInvoiceUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.GenerateInvoiceUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.PayInvoiceUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ProcessPatientPaymentUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ReadInvoiceUseCase;
import net.fmjaeschke.quantumhealth.application.ports.out.InsurancePolicyRepository;
import net.fmjaeschke.quantumhealth.application.ports.out.InvoiceRepository;
import net.fmjaeschke.quantumhealth.domain.model.EncounterId;
import net.fmjaeschke.quantumhealth.domain.model.Invoice;
import net.fmjaeschke.quantumhealth.domain.model.InvoiceId;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.time.Clock;

@ApplicationScoped
@Transactional
public class InvoiceService implements GenerateInvoiceUseCase, ReadInvoiceUseCase, PayInvoiceUseCase, DenyInvoiceUseCase,
        AppealInvoiceUseCase, ProcessPatientPaymentUseCase {

    private final InvoiceRepository invoiceRepository;
    private final InsurancePolicyRepository insurancePolicyRepository;
    private final BigDecimal consultationFee;
    private final Clock clock;

    public InvoiceService(InvoiceRepository invoiceRepository, InsurancePolicyRepository insurancePolicyRepository,
                          @ConfigProperty(name = "quantum-health.billing.consultation-fee", defaultValue = "150.00")
                          BigDecimal consultationFee, Clock clock) {
        this.invoiceRepository = invoiceRepository;
        this.insurancePolicyRepository = insurancePolicyRepository;
        this.consultationFee = consultationFee;
        this.clock = clock;
    }

    @Override
    public Invoice generate(EncounterId encounterId, PatientId patientId) {
        var policy = insurancePolicyRepository.findByPatientId(patientId).orElse(null);
        var draft = Invoice.draft(encounterId, patientId, consultationFee).calculateSplit(policy);
        invoiceRepository.saveNew(draft);
        return invoiceRepository.save(draft.submit());
    }

    @Override
    public Invoice findById(InvoiceId id) {
        return invoiceRepository.findById(id).orElseThrow(() -> new InvoiceNotFoundException(id));
    }

    @Override
    public Invoice pay(InvoiceId id) {
        var invoice = invoiceRepository.findById(id).orElseThrow(() -> new InvoiceNotFoundException(id));
        return invoiceRepository.save(invoice.pay());
    }

    @Override
    public Invoice deny(InvoiceId id) {
        var invoice = invoiceRepository.findById(id).orElseThrow(() -> new InvoiceNotFoundException(id));
        return invoiceRepository.save(invoice.deny());
    }

    @Override
    public Invoice appeal(InvoiceId id) {
        var invoice = invoiceRepository.findById(id).orElseThrow(() -> new InvoiceNotFoundException(id));
        return invoiceRepository.save(invoice.appeal());
    }

    @Override
    public Invoice processPatientPayment(InvoiceId id) {
        var invoice = invoiceRepository.findById(id).orElseThrow(() -> new InvoiceNotFoundException(id));
        return invoiceRepository.save(invoice.processPatientPayment(clock.instant()));
    }
}
