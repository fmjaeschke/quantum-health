package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import net.fmjaeschke.quantumhealth.application.exception.ConcurrentModificationException;
import net.fmjaeschke.quantumhealth.application.exception.PrescriptionNotFoundException;
import net.fmjaeschke.quantumhealth.application.ports.in.CancelPrescriptionUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.FulfillPrescriptionUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.IssuePrescriptionUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ListPrescriptionsUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ReadPrescriptionUseCase;
import net.fmjaeschke.quantumhealth.domain.exception.InvalidPrescriptionStateException;
import net.fmjaeschke.quantumhealth.domain.model.Disposition;
import net.fmjaeschke.quantumhealth.domain.model.MedicationItem;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.Prescription;
import net.fmjaeschke.quantumhealth.domain.model.PrescriptionId;
import net.fmjaeschke.quantumhealth.domain.model.PrescriptionPage;
import net.fmjaeschke.quantumhealth.domain.model.PrescriptionStatus;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@QuarkusTest
class PrescriptionResourceTest {

    static final UUID RX_ID       = UUID.fromString("cccccccc-0000-0000-0000-000000000001");
    static final UUID PATIENT_UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    static final List<MedicationItem> ITEMS = List.of(
            new MedicationItem("Aspirin", "100mg", "once daily"));

    static final Prescription ISSUED = Prescription.reconstitute(
            PrescriptionId.of(RX_ID),
            PatientId.of(PATIENT_UUID),
            "Alice Smith",
            UserId.of("dr-smith"),
            "Dr. Smith",
            ITEMS,
            Instant.parse("2026-01-15T10:00:00Z"),
            Disposition.issued(),
            0L);

    @InjectMock IssuePrescriptionUseCase issueMock;
    @InjectMock ReadPrescriptionUseCase readMock;
    @InjectMock ListPrescriptionsUseCase listMock;
    @InjectMock FulfillPrescriptionUseCase fulfillMock;
    @InjectMock CancelPrescriptionUseCase cancelMock;

    @Test
    @TestSecurity(user = "dr-smith", roles = {"DOCTOR"})
    void doctor_can_issue_prescription_and_gets_hal_response() {
        when(issueMock.issue(any(), any(), any())).thenReturn(ISSUED);

        given().contentType(ContentType.JSON)
                .body("""
                        {"patientId":"%s","medications":[{"drugName":"Aspirin","dosage":"100mg","frequency":"once daily"}]}
                        """.formatted(PATIENT_UUID))
                .when()
                .post("/prescriptions")
                .then()
                .statusCode(201)
                .header("Location", containsString("/prescriptions/"))
                .body("patientName", equalTo("Alice Smith"))
                .body("status", equalTo("ISSUED"))
                .body("medications", hasSize(1))
                .body("medications[0].drugName", equalTo("Aspirin"))
                .body("_links.self.href", containsString("/prescriptions/" + RX_ID));
    }

    @Test
    @TestSecurity(user = "pharmacist-1", roles = {"PHARMACIST"})
    void pharmacist_cannot_issue_prescription() {
        given().contentType(ContentType.JSON)
                .body("""
                        {"patientId":"%s","medications":[{"drugName":"Aspirin","dosage":"100mg","frequency":"once daily"}]}
                        """.formatted(PATIENT_UUID))
                .when()
                .post("/prescriptions")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "pharmacist-1", roles = {"PHARMACIST"})
    void pharmacist_can_read_issued_prescription_with_fulfill_link_and_no_cancel_link() {
        when(readMock.findById(eq(PrescriptionId.of(RX_ID)), any())).thenReturn(ISSUED);

        given().when()
                .get("/prescriptions/" + RX_ID)
                .then()
                .statusCode(200)
                .body("patientName", equalTo("Alice Smith"))
                .body("doctorName", equalTo("Dr. Smith"))
                .body("status", equalTo("ISSUED"))
                .body("medications", hasSize(1))
                .body("expiredAt", nullValue())
                .body("_links.'fulfill-prescription'.href", containsString("/fulfill"))
                .body("_links", not(hasKey("cancel-prescription")));
    }

    @Test
    @TestSecurity(user = "dr-smith", roles = {"DOCTOR"})
    void doctor_can_read_issued_prescription_with_cancel_link_and_no_fulfill_link() {
        when(readMock.findById(eq(PrescriptionId.of(RX_ID)), any())).thenReturn(ISSUED);

        given().when()
                .get("/prescriptions/" + RX_ID)
                .then()
                .statusCode(200)
                .body("status", equalTo("ISSUED"))
                .body("_links.'cancel-prescription'.href", containsString("/cancel"))
                .body("_links", not(hasKey("fulfill-prescription")));
    }

    @Test
    @TestSecurity(user = "dr-jones", roles = {"DOCTOR"})
    void doctor_cannot_see_cancel_link_on_colleagues_prescription() {
        when(readMock.findById(eq(PrescriptionId.of(RX_ID)), any())).thenReturn(ISSUED);

        given().when()
                .get("/prescriptions/" + RX_ID)
                .then()
                .statusCode(200)
                .body("status", equalTo("ISSUED"))
                .body("_links", not(hasKey("cancel-prescription")));
    }

    @Test
    @TestSecurity(user = "admin-1", roles = {"ADMIN"})
    void admin_can_see_cancel_link_on_any_issued_prescription() {
        when(readMock.findById(eq(PrescriptionId.of(RX_ID)), any())).thenReturn(ISSUED);

        given().when()
                .get("/prescriptions/" + RX_ID)
                .then()
                .statusCode(200)
                .body("status", equalTo("ISSUED"))
                .body("_links.'cancel-prescription'.href", containsString("/cancel"));
    }

    @Test
    @TestSecurity(user = "pharmacist-1", roles = {"PHARMACIST"})
    void get_unknown_prescription_returns_404() {
        when(readMock.findById(eq(PrescriptionId.of(RX_ID)), any()))
                .thenThrow(new PrescriptionNotFoundException(PrescriptionId.of(RX_ID)));

        given().when()
                .get("/prescriptions/" + RX_ID)
                .then()
                .statusCode(404)
                .body("status", equalTo(404));
    }

    @Test
    @TestSecurity(user = "dr-jones", roles = {"DOCTOR"})
    void foreign_doctor_gets_404_on_get_by_id() {
        when(readMock.findById(eq(PrescriptionId.of(RX_ID)), any()))
                .thenThrow(new PrescriptionNotFoundException(PrescriptionId.of(RX_ID)));

        given().when()
                .get("/prescriptions/" + RX_ID)
                .then()
                .statusCode(404)
                .body("status", equalTo(404));
    }

    @Test
    @TestSecurity(user = "dr-smith", roles = {"DOCTOR"})
    void issue_with_missing_patient_id_returns_400() {
        given().contentType(ContentType.JSON)
                .body("""
                        {"medications":[{"drugName":"Aspirin","dosage":"100mg","frequency":"once daily"}]}
                        """)
                .when()
                .post("/prescriptions")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "dr-smith", roles = {"DOCTOR"})
    void issue_with_empty_medications_returns_400() {
        given().contentType(ContentType.JSON)
                .body("""
                        {"patientId":"%s","medications":[]}
                        """.formatted(PATIENT_UUID))
                .when()
                .post("/prescriptions")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "pharmacist-1", roles = {"PHARMACIST"})
    void pharmacist_can_list_prescriptions() {
        var page = new PrescriptionPage(List.of(ISSUED), 1L, 0, 20);
        when(listMock.list(anyInt(), anyInt(), any())).thenReturn(page);

        given().when()
                .get("/prescriptions")
                .then()
                .statusCode(200)
                .body("_embedded.prescriptions", hasSize(1))
                .body("_embedded.prescriptions[0].patientName", equalTo("Alice Smith"))
                .body("totalElements", equalTo(1))
                .body("_links.self.href", containsString("/prescriptions"));
    }

    @Test
    @TestSecurity(user = "dr-smith", roles = {"DOCTOR"})
    void doctor_can_list_prescriptions() {
        var page = new PrescriptionPage(List.of(ISSUED), 1L, 0, 20);
        when(listMock.list(anyInt(), anyInt(), any())).thenReturn(page);

        given().when()
                .get("/prescriptions")
                .then()
                .statusCode(200)
                .body("_embedded.prescriptions", hasSize(1));
    }

    @Test
    @TestSecurity(user = "admin-1", roles = {"ADMIN"})
    void admin_can_list_prescriptions() {
        var page = new PrescriptionPage(List.of(ISSUED), 1L, 0, 20);
        when(listMock.list(anyInt(), anyInt(), any())).thenReturn(page);

        given().when()
                .get("/prescriptions")
                .then()
                .statusCode(200)
                .body("_embedded.prescriptions", hasSize(1));
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void clerk_cannot_list_prescriptions() {
        given().when()
                .get("/prescriptions")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "pharmacist-1", roles = {"PHARMACIST"})
    void pharmacist_cannot_see_fulfill_link_on_expired_prescription() {
        var expired = Prescription.reconstitute(
                PrescriptionId.of(RX_ID), PatientId.of(PATIENT_UUID), "Alice Smith",
                UserId.of("dr-smith"), "Dr. Smith", ITEMS,
                Instant.parse("2026-01-15T10:00:00Z"),
                Disposition.reconstituted(PrescriptionStatus.EXPIRED,
                        null, null, null, null, null, Instant.parse("2026-02-16T00:00:00Z")),
                0L);
        when(readMock.findById(eq(PrescriptionId.of(RX_ID)), any())).thenReturn(expired);

        given().when()
                .get("/prescriptions/" + RX_ID)
                .then()
                .statusCode(200)
                .body("status", equalTo("EXPIRED"))
                .body("expiredAt", equalTo("2026-02-16T00:00:00Z"))
                .body("_links", not(hasKey("fulfill-prescription")));
    }

    @Test
    @TestSecurity(user = "pharmacist-1", roles = {"PHARMACIST"})
    void pharmacist_cannot_see_fulfill_link_on_fulfilled_prescription() {
        var fulfilled = Prescription.reconstitute(
                PrescriptionId.of(RX_ID), PatientId.of(PATIENT_UUID), "Alice Smith",
                UserId.of("dr-smith"), "Dr. Smith", ITEMS,
                Instant.parse("2026-01-15T10:00:00Z"),
                Disposition.reconstituted(PrescriptionStatus.FULFILLED,
                        Instant.parse("2026-01-16T09:00:00Z"), UserId.of("pharmacist-1"),
                        null, null, null, null),
                0L);
        when(readMock.findById(eq(PrescriptionId.of(RX_ID)), any())).thenReturn(fulfilled);

        given().when()
                .get("/prescriptions/" + RX_ID)
                .then()
                .statusCode(200)
                .body("status", equalTo("FULFILLED"))
                .body("_links", not(hasKey("fulfill-prescription")));
    }

    @Test
    @TestSecurity(user = "pharmacist-1", roles = {"PHARMACIST"})
    void pharmacist_can_fulfill_issued_prescription() {
        when(fulfillMock.fulfill(eq(PrescriptionId.of(RX_ID)), any()))
                .thenReturn(ISSUED.fulfill(UserId.of("pharmacist-1"), Instant.now()));

        given().when()
                .post("/prescriptions/" + RX_ID + "/fulfill")
                .then()
                .statusCode(200)
                .body("status", equalTo("FULFILLED"));
    }

    @Test
    @TestSecurity(user = "dr-smith", roles = {"DOCTOR"})
    void doctor_cannot_fulfill_prescription() {
        given().when()
                .post("/prescriptions/" + RX_ID + "/fulfill")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "pharmacist-1", roles = {"PHARMACIST"})
    void fulfilling_already_fulfilled_prescription_returns_409() {
        when(fulfillMock.fulfill(eq(PrescriptionId.of(RX_ID)), any()))
                .thenThrow(new InvalidPrescriptionStateException("fulfill", PrescriptionStatus.FULFILLED));

        given().when()
                .post("/prescriptions/" + RX_ID + "/fulfill")
                .then()
                .statusCode(409);
    }

    @Test
    @TestSecurity(user = "pharmacist-1", roles = {"PHARMACIST"})
    void fulfilling_concurrently_modified_prescription_returns_409() {
        when(fulfillMock.fulfill(eq(PrescriptionId.of(RX_ID)), any()))
                .thenThrow(new ConcurrentModificationException("stale version"));

        given().when()
                .post("/prescriptions/" + RX_ID + "/fulfill")
                .then()
                .statusCode(409);
    }

    @Test
    @TestSecurity(user = "dr-smith", roles = {"DOCTOR"})
    void doctor_can_cancel_own_prescription() {
        when(cancelMock.cancel(eq(PrescriptionId.of(RX_ID)), any(), any()))
                .thenReturn(ISSUED.cancel(UserId.of("dr-smith"), "Prescribing error", Instant.now()));

        given().contentType(ContentType.JSON)
                .body("""
                        {"reason":"Prescribing error"}
                        """)
                .when()
                .post("/prescriptions/" + RX_ID + "/cancel")
                .then()
                .statusCode(200)
                .body("status", equalTo("CANCELLED"));
    }

    @Test
    @TestSecurity(user = "pharmacist-1", roles = {"PHARMACIST"})
    void pharmacist_cannot_cancel_prescription() {
        given().contentType(ContentType.JSON)
                .body("""
                        {"reason":"Prescribing error"}
                        """)
                .when()
                .post("/prescriptions/" + RX_ID + "/cancel")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin-1", roles = {"ADMIN"})
    void admin_can_cancel_any_prescription() {
        when(cancelMock.cancel(eq(PrescriptionId.of(RX_ID)), any(), any()))
                .thenReturn(ISSUED.cancel(UserId.of("admin-1"), "Admin correction", Instant.now()));

        given().contentType(ContentType.JSON)
                .body("""
                        {"reason":"Admin correction"}
                        """)
                .when()
                .post("/prescriptions/" + RX_ID + "/cancel")
                .then()
                .statusCode(200)
                .body("status", equalTo("CANCELLED"));
    }

    @Test
    @TestSecurity(user = "dr-smith", roles = {"DOCTOR"})
    void cancelling_already_cancelled_prescription_returns_409() {
        when(cancelMock.cancel(eq(PrescriptionId.of(RX_ID)), any(), any()))
                .thenThrow(new InvalidPrescriptionStateException("cancel", PrescriptionStatus.CANCELLED));

        given().contentType(ContentType.JSON)
                .body("""
                        {"reason":"Prescribing error"}
                        """)
                .when()
                .post("/prescriptions/" + RX_ID + "/cancel")
                .then()
                .statusCode(409);
    }

    @Test
    @TestSecurity(user = "dr-smith", roles = {"DOCTOR"})
    void cancel_with_reason_over_500_chars_returns_400() {
        var tooLong = "x".repeat(501);

        given().contentType(ContentType.JSON)
                .body("""
                        {"reason":"%s"}
                        """.formatted(tooLong))
                .when()
                .post("/prescriptions/" + RX_ID + "/cancel")
                .then()
                .statusCode(400);
    }
}
