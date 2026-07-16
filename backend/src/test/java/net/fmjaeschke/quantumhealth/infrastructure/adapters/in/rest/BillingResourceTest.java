package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import net.fmjaeschke.quantumhealth.application.exception.InvoiceNotFoundException;
import net.fmjaeschke.quantumhealth.application.ports.in.AppealInvoiceUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.DenyInvoiceUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.PayInvoiceUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ProcessPatientPaymentUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ReadInvoiceUseCase;
import net.fmjaeschke.quantumhealth.domain.exception.InvalidInvoiceStateException;
import net.fmjaeschke.quantumhealth.domain.model.EncounterId;
import net.fmjaeschke.quantumhealth.domain.model.Invoice;
import net.fmjaeschke.quantumhealth.domain.model.InvoiceId;
import net.fmjaeschke.quantumhealth.domain.model.InvoiceStatus;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@QuarkusTest
class BillingResourceTest {

    static final UUID INVOICE_UUID = UUID.fromString("dddddddd-0000-0000-0000-000000000001");

    static final Invoice SUBMITTED = Invoice.reconstitute(
            InvoiceId.of(INVOICE_UUID), EncounterId.generate(), PatientId.generate(),
            new BigDecimal("150.00"), BigDecimal.ZERO, new BigDecimal("150.00"),
            InvoiceStatus.SUBMITTED, null);

    static final Invoice PAID = Invoice.reconstitute(
            InvoiceId.of(INVOICE_UUID), EncounterId.generate(), PatientId.generate(),
            new BigDecimal("150.00"), BigDecimal.ZERO, new BigDecimal("150.00"),
            InvoiceStatus.PAID, null);

    static final Invoice DENIED = Invoice.reconstitute(
            InvoiceId.of(INVOICE_UUID), EncounterId.generate(), PatientId.generate(),
            new BigDecimal("150.00"), BigDecimal.ZERO, new BigDecimal("150.00"),
            InvoiceStatus.DENIED, null);

    static final Invoice APPEALED = Invoice.reconstitute(
            InvoiceId.of(INVOICE_UUID), EncounterId.generate(), PatientId.generate(),
            new BigDecimal("150.00"), BigDecimal.ZERO, new BigDecimal("150.00"),
            InvoiceStatus.APPEALED, null);

    static final Invoice PATIENT_PAID = Invoice.reconstitute(
            InvoiceId.of(INVOICE_UUID), EncounterId.generate(), PatientId.generate(),
            new BigDecimal("150.00"), BigDecimal.ZERO, new BigDecimal("150.00"),
            InvoiceStatus.SUBMITTED, Instant.now());

    static final Invoice ZERO_COPAY = Invoice.reconstitute(
            InvoiceId.of(INVOICE_UUID), EncounterId.generate(), PatientId.generate(),
            new BigDecimal("150.00"), new BigDecimal("150.00"), BigDecimal.ZERO,
            InvoiceStatus.SUBMITTED, null);

    @InjectMock ReadInvoiceUseCase readMock;
    @InjectMock PayInvoiceUseCase payMock;
    @InjectMock DenyInvoiceUseCase denyMock;
    @InjectMock AppealInvoiceUseCase appealMock;
    @InjectMock ProcessPatientPaymentUseCase processPatientPaymentMock;

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void clerk_can_read_invoice_as_hal() {
        when(readMock.findById(eq(InvoiceId.of(INVOICE_UUID)))).thenReturn(SUBMITTED);

        given().when()
                .get("/invoices/" + INVOICE_UUID)
                .then()
                .statusCode(200)
                .body("status", equalTo("SUBMITTED"))
                .body("totalAmount", equalTo(150.00f))
                .body("_links.self.href", org.hamcrest.Matchers.containsString("/invoices/" + INVOICE_UUID));
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void clerk_can_read_invoice_with_versioned_media_type() {
        when(readMock.findById(eq(InvoiceId.of(INVOICE_UUID)))).thenReturn(SUBMITTED);

        given().accept("application/vnd.quantumhealth.v1+json")
                .when()
                .get("/invoices/" + INVOICE_UUID)
                .then()
                .statusCode(200)
                .body("status", equalTo("SUBMITTED"));
    }

    @Test
    @TestSecurity(user = "dr-smith", roles = {"DOCTOR"})
    void doctor_cannot_read_invoice() {
        given().when()
                .get("/invoices/" + INVOICE_UUID)
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void get_unknown_invoice_returns_404() {
        when(readMock.findById(eq(InvoiceId.of(INVOICE_UUID))))
                .thenThrow(new InvoiceNotFoundException(InvoiceId.of(INVOICE_UUID)));

        given().when()
                .get("/invoices/" + INVOICE_UUID)
                .then()
                .statusCode(404)
                .body("status", equalTo(404));
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void clerk_can_pay_submitted_invoice() {
        when(payMock.pay(eq(InvoiceId.of(INVOICE_UUID)))).thenReturn(PAID);

        given().when()
                .post("/invoices/" + INVOICE_UUID + "/pay")
                .then()
                .statusCode(200)
                .body("status", equalTo("PAID"));
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void clerk_can_deny_submitted_invoice() {
        when(denyMock.deny(eq(InvoiceId.of(INVOICE_UUID)))).thenReturn(DENIED);

        given().when()
                .post("/invoices/" + INVOICE_UUID + "/deny")
                .then()
                .statusCode(200)
                .body("status", equalTo("DENIED"));
    }

    @Test
    @TestSecurity(user = "dr-smith", roles = {"DOCTOR"})
    void doctor_cannot_pay_invoice() {
        given().when()
                .post("/invoices/" + INVOICE_UUID + "/pay")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "dr-smith", roles = {"DOCTOR"})
    void doctor_cannot_deny_invoice() {
        given().when()
                .post("/invoices/" + INVOICE_UUID + "/deny")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void paying_non_submitted_invoice_returns_409() {
        when(payMock.pay(eq(InvoiceId.of(INVOICE_UUID))))
                .thenThrow(new InvalidInvoiceStateException("pay", InvoiceStatus.PAID));

        given().when()
                .post("/invoices/" + INVOICE_UUID + "/pay")
                .then()
                .statusCode(409);
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void clerk_can_appeal_denied_invoice() {
        when(appealMock.appeal(eq(InvoiceId.of(INVOICE_UUID)))).thenReturn(APPEALED);

        given().when()
                .post("/invoices/" + INVOICE_UUID + "/appeal")
                .then()
                .statusCode(200)
                .body("status", equalTo("APPEALED"));
    }

    @Test
    @TestSecurity(user = "dr-smith", roles = {"DOCTOR"})
    void doctor_cannot_appeal_invoice() {
        given().when()
                .post("/invoices/" + INVOICE_UUID + "/appeal")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void appealing_non_denied_invoice_returns_409() {
        when(appealMock.appeal(eq(InvoiceId.of(INVOICE_UUID))))
                .thenThrow(new InvalidInvoiceStateException("appeal", InvoiceStatus.SUBMITTED));

        given().when()
                .post("/invoices/" + INVOICE_UUID + "/appeal")
                .then()
                .statusCode(409);
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void appeal_denial_link_present_only_when_denied() {
        when(readMock.findById(eq(InvoiceId.of(INVOICE_UUID)))).thenReturn(DENIED);

        given().when()
                .get("/invoices/" + INVOICE_UUID)
                .then()
                .statusCode(200)
                .body("_links.appeal-denial.href", org.hamcrest.Matchers.containsString("/invoices/" + INVOICE_UUID + "/appeal"));
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void appeal_denial_link_absent_when_submitted() {
        when(readMock.findById(eq(InvoiceId.of(INVOICE_UUID)))).thenReturn(SUBMITTED);

        given().when()
                .get("/invoices/" + INVOICE_UUID)
                .then()
                .statusCode(200)
                .body("_links.appeal-denial", org.hamcrest.Matchers.nullValue());
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void appeal_denial_link_absent_when_paid() {
        when(readMock.findById(eq(InvoiceId.of(INVOICE_UUID)))).thenReturn(PAID);

        given().when()
                .get("/invoices/" + INVOICE_UUID)
                .then()
                .statusCode(200)
                .body("_links.appeal-denial", org.hamcrest.Matchers.nullValue());
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void appeal_denial_link_absent_when_appealed() {
        when(readMock.findById(eq(InvoiceId.of(INVOICE_UUID)))).thenReturn(APPEALED);

        given().when()
                .get("/invoices/" + INVOICE_UUID)
                .then()
                .statusCode(200)
                .body("_links.appeal-denial", org.hamcrest.Matchers.nullValue());
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void clerk_can_process_patient_payment_regardless_of_status() {
        when(processPatientPaymentMock.processPatientPayment(eq(InvoiceId.of(INVOICE_UUID)))).thenReturn(PATIENT_PAID);

        given().when()
                .post("/invoices/" + INVOICE_UUID + "/process-patient-payment")
                .then()
                .statusCode(200)
                .body("patientPaidAt", org.hamcrest.Matchers.notNullValue());
    }

    @Test
    @TestSecurity(user = "dr-smith", roles = {"DOCTOR"})
    void doctor_cannot_process_patient_payment() {
        given().when()
                .post("/invoices/" + INVOICE_UUID + "/process-patient-payment")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void processing_already_paid_copay_returns_409() {
        when(processPatientPaymentMock.processPatientPayment(eq(InvoiceId.of(INVOICE_UUID))))
                .thenThrow(new InvalidInvoiceStateException("process-patient-payment", InvoiceStatus.SUBMITTED));

        given().when()
                .post("/invoices/" + INVOICE_UUID + "/process-patient-payment")
                .then()
                .statusCode(409);
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void process_patient_payment_link_present_when_copay_owed_and_unpaid() {
        when(readMock.findById(eq(InvoiceId.of(INVOICE_UUID)))).thenReturn(SUBMITTED);

        given().when()
                .get("/invoices/" + INVOICE_UUID)
                .then()
                .statusCode(200)
                .body("_links.process-patient-payment.href",
                        org.hamcrest.Matchers.containsString("/invoices/" + INVOICE_UUID + "/process-patient-payment"));
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void process_patient_payment_link_absent_when_already_paid() {
        when(readMock.findById(eq(InvoiceId.of(INVOICE_UUID)))).thenReturn(PATIENT_PAID);

        given().when()
                .get("/invoices/" + INVOICE_UUID)
                .then()
                .statusCode(200)
                .body("_links.process-patient-payment", org.hamcrest.Matchers.nullValue());
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void process_patient_payment_link_absent_when_zero_copay() {
        when(readMock.findById(eq(InvoiceId.of(INVOICE_UUID)))).thenReturn(ZERO_COPAY);

        given().when()
                .get("/invoices/" + INVOICE_UUID)
                .then()
                .statusCode(200)
                .body("_links.process-patient-payment", org.hamcrest.Matchers.nullValue());
    }
}
