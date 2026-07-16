package net.fmjaeschke.quantumhealth.application.usecase;

import net.fmjaeschke.quantumhealth.application.exception.InvoiceNotFoundException;
import net.fmjaeschke.quantumhealth.application.ports.out.InsurancePolicyRepository;
import net.fmjaeschke.quantumhealth.application.ports.out.InvoiceRepository;
import net.fmjaeschke.quantumhealth.domain.model.EncounterId;
import net.fmjaeschke.quantumhealth.domain.model.InsurancePolicy;
import net.fmjaeschke.quantumhealth.domain.model.InsuranceTier;
import net.fmjaeschke.quantumhealth.domain.model.Invoice;
import net.fmjaeschke.quantumhealth.domain.model.InvoiceId;
import net.fmjaeschke.quantumhealth.domain.model.InvoiceStatus;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvoiceServiceTest {

    static final EncounterId ENCOUNTER = EncounterId.generate();
    static final PatientId PATIENT = PatientId.generate();
    static final BigDecimal FEE = new BigDecimal("150.00");

    @Test
    void generate_with_gold_policy_persists_submitted_invoice_with_90_percent_coverage() {
        var invoices = new FakeInvoiceRepo();
        var policies = new FakeInsurancePolicyRepo(new InsurancePolicy(PATIENT, InsuranceTier.GOLD));
        var service = new InvoiceService(invoices, policies, FEE);

        var invoice = service.generate(ENCOUNTER, PATIENT);

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.SUBMITTED);
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo("150.00");
        assertThat(invoice.getInsurerAmount()).isEqualByComparingTo("135.00");
        assertThat(invoice.getPatientCopay()).isEqualByComparingTo("15.00");
        assertThat(invoice.getEncounterId()).isEqualTo(ENCOUNTER);
        assertThat(invoice.getPatientId()).isEqualTo(PATIENT);
    }

    @Test
    void generate_without_insurance_policy_persists_submitted_invoice_with_full_copay() {
        var invoices = new FakeInvoiceRepo();
        var policies = new FakeInsurancePolicyRepo();
        var service = new InvoiceService(invoices, policies, FEE);

        var invoice = service.generate(ENCOUNTER, PATIENT);

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.SUBMITTED);
        assertThat(invoice.getInsurerAmount()).isEqualByComparingTo("0.00");
        assertThat(invoice.getPatientCopay()).isEqualByComparingTo("150.00");
    }

    @Test
    void generate_persists_exactly_one_invoice() {
        var invoices = new FakeInvoiceRepo();
        var service = new InvoiceService(invoices, new FakeInsurancePolicyRepo(), FEE);

        service.generate(ENCOUNTER, PATIENT);

        assertThat(invoices.store).hasSize(1);
        assertThat(invoices.store.get(0).getEncounterId()).isEqualTo(ENCOUNTER);
    }

    @Test
    void findById_returns_persisted_invoice() {
        var invoices = new FakeInvoiceRepo();
        var service = new InvoiceService(invoices, new FakeInsurancePolicyRepo(), FEE);
        var generated = service.generate(ENCOUNTER, PATIENT);

        var found = service.findById(generated.getId());

        assertThat(found.getId()).isEqualTo(generated.getId());
    }

    @Test
    void findById_throws_when_not_found() {
        var service = new InvoiceService(new FakeInvoiceRepo(), new FakeInsurancePolicyRepo(), FEE);

        assertThatThrownBy(() -> service.findById(InvoiceId.generate()))
                .isInstanceOf(InvoiceNotFoundException.class);
    }

    @Test
    void pay_persists_paid_status() {
        var invoices = new FakeInvoiceRepo();
        var service = new InvoiceService(invoices, new FakeInsurancePolicyRepo(), FEE);
        var generated = service.generate(ENCOUNTER, PATIENT);

        var paid = service.pay(generated.getId());

        assertThat(paid.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(service.findById(generated.getId()).getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void deny_persists_denied_status() {
        var invoices = new FakeInvoiceRepo();
        var service = new InvoiceService(invoices, new FakeInsurancePolicyRepo(), FEE);
        var generated = service.generate(ENCOUNTER, PATIENT);

        var denied = service.deny(generated.getId());

        assertThat(denied.getStatus()).isEqualTo(InvoiceStatus.DENIED);
        assertThat(service.findById(generated.getId()).getStatus()).isEqualTo(InvoiceStatus.DENIED);
    }

    @Test
    void pay_throws_when_not_found() {
        var service = new InvoiceService(new FakeInvoiceRepo(), new FakeInsurancePolicyRepo(), FEE);

        assertThatThrownBy(() -> service.pay(InvoiceId.generate()))
                .isInstanceOf(InvoiceNotFoundException.class);
    }

    @Test
    void deny_throws_when_not_found() {
        var service = new InvoiceService(new FakeInvoiceRepo(), new FakeInsurancePolicyRepo(), FEE);

        assertThatThrownBy(() -> service.deny(InvoiceId.generate()))
                .isInstanceOf(InvoiceNotFoundException.class);
    }

    @Test
    void appeal_persists_appealed_status() {
        var invoices = new FakeInvoiceRepo();
        var service = new InvoiceService(invoices, new FakeInsurancePolicyRepo(), FEE);
        var generated = service.generate(ENCOUNTER, PATIENT);
        service.deny(generated.getId());

        var appealed = service.appeal(generated.getId());

        assertThat(appealed.getStatus()).isEqualTo(InvoiceStatus.APPEALED);
        assertThat(service.findById(generated.getId()).getStatus()).isEqualTo(InvoiceStatus.APPEALED);
    }

    @Test
    void appeal_throws_when_not_found() {
        var service = new InvoiceService(new FakeInvoiceRepo(), new FakeInsurancePolicyRepo(), FEE);

        assertThatThrownBy(() -> service.appeal(InvoiceId.generate()))
                .isInstanceOf(InvoiceNotFoundException.class);
    }

    @Test
    void processPatientPayment_persists_patientPaidAt() {
        var invoices = new FakeInvoiceRepo();
        var service = new InvoiceService(invoices, new FakeInsurancePolicyRepo(), FEE);
        var generated = service.generate(ENCOUNTER, PATIENT);

        var paid = service.processPatientPayment(generated.getId());

        assertThat(paid.getPatientPaidAt()).isPresent();
        assertThat(service.findById(generated.getId()).getPatientPaidAt()).isPresent();
    }

    @Test
    void processPatientPayment_throws_when_not_found() {
        var service = new InvoiceService(new FakeInvoiceRepo(), new FakeInsurancePolicyRepo(), FEE);

        assertThatThrownBy(() -> service.processPatientPayment(InvoiceId.generate()))
                .isInstanceOf(InvoiceNotFoundException.class);
    }

    // --- fakes ---

    static class FakeInvoiceRepo implements InvoiceRepository {
        final List<Invoice> store = new ArrayList<>();

        @Override
        public Invoice saveNew(Invoice invoice) {
            store.add(invoice);
            return invoice;
        }

        @Override
        public Invoice save(Invoice invoice) {
            store.removeIf(i -> i.getId().equals(invoice.getId()));
            store.add(invoice);
            return invoice;
        }

        @Override
        public Optional<Invoice> findById(InvoiceId id) {
            return store.stream().filter(i -> i.getId().equals(id)).findFirst();
        }

        @Override
        public Optional<Invoice> findByEncounterId(EncounterId encounterId) {
            return store.stream().filter(i -> i.getEncounterId().equals(encounterId)).findFirst();
        }
    }

    static class FakeInsurancePolicyRepo implements InsurancePolicyRepository {
        private final List<InsurancePolicy> policies;

        FakeInsurancePolicyRepo(InsurancePolicy... policies) {
            this.policies = List.of(policies);
        }

        @Override
        public Optional<InsurancePolicy> findByPatientId(PatientId patientId) {
            return policies.stream().filter(p -> p.patientId().equals(patientId)).findFirst();
        }
    }
}
