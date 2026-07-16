package net.fmjaeschke.quantumhealth.domain.model;

import net.fmjaeschke.quantumhealth.domain.exception.InvalidInvoiceStateException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvoiceTest {

    private static final EncounterId ENCOUNTER = EncounterId.generate();
    private static final PatientId PATIENT = PatientId.generate();

    @Test
    void draft_creates_invoice_with_draft_status_and_zeroed_split() {
        var invoice = Invoice.draft(ENCOUNTER, PATIENT, new BigDecimal("150.00"));

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo("150.00");
        assertThat(invoice.getInsurerAmount()).isEqualByComparingTo("0.00");
        assertThat(invoice.getPatientCopay()).isEqualByComparingTo("0.00");
        assertThat(invoice.getPatientPaidAt()).isEmpty();
    }

    @Test
    void calculateSplit_with_gold_policy_covers_90_percent() {
        var invoice = Invoice.draft(ENCOUNTER, PATIENT, new BigDecimal("150.00"))
                .calculateSplit(new InsurancePolicy(PATIENT, InsuranceTier.GOLD));

        assertThat(invoice.getInsurerAmount()).isEqualByComparingTo("135.00");
        assertThat(invoice.getPatientCopay()).isEqualByComparingTo("15.00");
    }

    @Test
    void calculateSplit_with_silver_policy_covers_70_percent() {
        var invoice = Invoice.draft(ENCOUNTER, PATIENT, new BigDecimal("150.00"))
                .calculateSplit(new InsurancePolicy(PATIENT, InsuranceTier.SILVER));

        assertThat(invoice.getInsurerAmount()).isEqualByComparingTo("105.00");
        assertThat(invoice.getPatientCopay()).isEqualByComparingTo("45.00");
    }

    @Test
    void calculateSplit_with_bronze_policy_covers_50_percent() {
        var invoice = Invoice.draft(ENCOUNTER, PATIENT, new BigDecimal("150.00"))
                .calculateSplit(new InsurancePolicy(PATIENT, InsuranceTier.BRONZE));

        assertThat(invoice.getInsurerAmount()).isEqualByComparingTo("75.00");
        assertThat(invoice.getPatientCopay()).isEqualByComparingTo("75.00");
    }

    @Test
    void calculateSplit_with_no_policy_defaults_to_zero_coverage() {
        var invoice = Invoice.draft(ENCOUNTER, PATIENT, new BigDecimal("150.00"))
                .calculateSplit(null);

        assertThat(invoice.getInsurerAmount()).isEqualByComparingTo("0.00");
        assertThat(invoice.getPatientCopay()).isEqualByComparingTo("150.00");
    }

    @Test
    void calculateSplit_rounds_half_up_to_two_decimal_places() {
        var invoice = Invoice.draft(ENCOUNTER, PATIENT, new BigDecimal("99.99"))
                .calculateSplit(new InsurancePolicy(PATIENT, InsuranceTier.SILVER));

        // 99.99 * 0.70 = 69.993 -> rounds to 69.99, copay is the remainder (not independently rounded)
        assertThat(invoice.getInsurerAmount()).isEqualByComparingTo("69.99");
        assertThat(invoice.getPatientCopay()).isEqualByComparingTo("30.00");
        assertThat(invoice.getInsurerAmount().add(invoice.getPatientCopay())).isEqualByComparingTo("99.99");
    }

    @Test
    void submit_transitions_draft_to_submitted_preserving_split() {
        var invoice = Invoice.draft(ENCOUNTER, PATIENT, new BigDecimal("150.00"))
                .calculateSplit(new InsurancePolicy(PATIENT, InsuranceTier.GOLD));

        var submitted = invoice.submit();

        assertThat(submitted.getStatus()).isEqualTo(InvoiceStatus.SUBMITTED);
        assertThat(submitted.getInsurerAmount()).isEqualByComparingTo("135.00");
        assertThat(submitted.getPatientCopay()).isEqualByComparingTo("15.00");
        assertThat(submitted.getId()).isEqualTo(invoice.getId());
    }

    @Test
    void pay_transitions_submitted_to_paid() {
        var invoice = Invoice.draft(ENCOUNTER, PATIENT, new BigDecimal("150.00"))
                .calculateSplit(null)
                .submit();

        var paid = invoice.pay();

        assertThat(paid.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(paid.getId()).isEqualTo(invoice.getId());
    }

    @Test
    void deny_transitions_submitted_to_denied() {
        var invoice = Invoice.draft(ENCOUNTER, PATIENT, new BigDecimal("150.00"))
                .calculateSplit(null)
                .submit();

        var denied = invoice.deny();

        assertThat(denied.getStatus()).isEqualTo(InvoiceStatus.DENIED);
        assertThat(denied.getId()).isEqualTo(invoice.getId());
    }

    @Test
    void pay_on_non_submitted_invoice_throws() {
        var draft = Invoice.draft(ENCOUNTER, PATIENT, new BigDecimal("150.00"));

        assertThatThrownBy(draft::pay).isInstanceOf(InvalidInvoiceStateException.class);
    }

    @Test
    void deny_on_non_submitted_invoice_throws() {
        var draft = Invoice.draft(ENCOUNTER, PATIENT, new BigDecimal("150.00"));

        assertThatThrownBy(draft::deny).isInstanceOf(InvalidInvoiceStateException.class);
    }

    @Test
    void pay_on_already_paid_invoice_throws() {
        var paid = Invoice.draft(ENCOUNTER, PATIENT, new BigDecimal("150.00"))
                .calculateSplit(null)
                .submit()
                .pay();

        assertThatThrownBy(paid::pay).isInstanceOf(InvalidInvoiceStateException.class);
    }

    @Test
    void appeal_transitions_denied_to_appealed() {
        var denied = Invoice.draft(ENCOUNTER, PATIENT, new BigDecimal("150.00"))
                .calculateSplit(null)
                .submit()
                .deny();

        var appealed = denied.appeal();

        assertThat(appealed.getStatus()).isEqualTo(InvoiceStatus.APPEALED);
        assertThat(appealed.getId()).isEqualTo(denied.getId());
    }

    @Test
    void appeal_on_draft_invoice_throws() {
        var draft = Invoice.draft(ENCOUNTER, PATIENT, new BigDecimal("150.00"));

        assertThatThrownBy(draft::appeal).isInstanceOf(InvalidInvoiceStateException.class);
    }

    @Test
    void appeal_on_submitted_invoice_throws() {
        var submitted = Invoice.draft(ENCOUNTER, PATIENT, new BigDecimal("150.00"))
                .calculateSplit(null)
                .submit();

        assertThatThrownBy(submitted::appeal).isInstanceOf(InvalidInvoiceStateException.class);
    }

    @Test
    void appeal_on_paid_invoice_throws() {
        var paid = Invoice.draft(ENCOUNTER, PATIENT, new BigDecimal("150.00"))
                .calculateSplit(null)
                .submit()
                .pay();

        assertThatThrownBy(paid::appeal).isInstanceOf(InvalidInvoiceStateException.class);
    }

    @Test
    void appeal_on_already_appealed_invoice_throws() {
        var appealed = Invoice.draft(ENCOUNTER, PATIENT, new BigDecimal("150.00"))
                .calculateSplit(null)
                .submit()
                .deny()
                .appeal();

        assertThatThrownBy(appealed::appeal).isInstanceOf(InvalidInvoiceStateException.class);
    }

    @Test
    void processPatientPayment_sets_patientPaidAt_on_submitted_invoice() {
        var submitted = Invoice.draft(ENCOUNTER, PATIENT, new BigDecimal("150.00"))
                .calculateSplit(null)
                .submit();

        var paid = submitted.processPatientPayment();

        assertThat(paid.getPatientPaidAt()).isPresent();
        assertThat(paid.getStatus()).isEqualTo(InvoiceStatus.SUBMITTED);
    }

    @Test
    void processPatientPayment_works_regardless_of_denied_status() {
        var denied = Invoice.draft(ENCOUNTER, PATIENT, new BigDecimal("150.00"))
                .calculateSplit(null)
                .submit()
                .deny();

        var paid = denied.processPatientPayment();

        assertThat(paid.getPatientPaidAt()).isPresent();
        assertThat(paid.getStatus()).isEqualTo(InvoiceStatus.DENIED);
    }

    @Test
    void processPatientPayment_on_already_paid_copay_throws() {
        var paid = Invoice.draft(ENCOUNTER, PATIENT, new BigDecimal("150.00"))
                .calculateSplit(null)
                .submit()
                .processPatientPayment();

        assertThatThrownBy(paid::processPatientPayment).isInstanceOf(InvalidInvoiceStateException.class);
    }
}
