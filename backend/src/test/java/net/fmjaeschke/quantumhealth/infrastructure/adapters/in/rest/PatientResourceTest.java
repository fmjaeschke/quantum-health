package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import net.fmjaeschke.quantumhealth.application.exception.PatientNotFoundException;
import net.fmjaeschke.quantumhealth.application.ports.in.ListPatientUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ReadPatientUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.RegisterPatientUseCase;
import net.fmjaeschke.quantumhealth.domain.model.Patient;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.PatientPage;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@QuarkusTest
class PatientResourceTest {

    static final UUID PATIENT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    static final Patient ALICE = Patient.reconstitute(PatientId.of(PATIENT_ID), "Alice", "Smith", LocalDate.of(1990, 5, 15));

    @InjectMock
    RegisterPatientUseCase registerPatientMock;

    @InjectMock
    ReadPatientUseCase readPatientMock;

    @InjectMock
    ListPatientUseCase listPatientMock;

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void clerk_can_register_patient_and_gets_hal_response() {
        when(registerPatientMock.register(UserId.of("clerk-1"), "Alice", "Smith", LocalDate.of(1990, 5, 15))).thenReturn(ALICE);

        given().contentType(ContentType.JSON)
                .body("""
                        {"firstName":"Alice","lastName":"Smith","dateOfBirth":"1990-05-15"}
                        """)
                .when()
                .post("/patients")
                .then()
                .statusCode(201)
                .header("Location", containsString("/patients/"))
                .body("firstName", equalTo("Alice"))
                .body("_links.self.href", containsString("/patients/"));
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void clerk_can_read_patient_with_hal_links() {
        when(readPatientMock.findById(PatientId.of(PATIENT_ID), UserId.of("clerk-1"))).thenReturn(ALICE);

        given().when()
                .get("/patients/" + PATIENT_ID)
                .then()
                .statusCode(200)
                .body("firstName", equalTo("Alice"))
                .body("lastName", equalTo("Smith"))
                .body("dateOfBirth", equalTo("1990-05-15"))
                .body("_links.self.href", containsString("/patients/" + PATIENT_ID));
    }

    @Test
    @TestSecurity(user = "nurse-1", roles = {"NURSE"})
    void nurse_cannot_register_patient() {
        given().contentType(ContentType.JSON)
                .body("{\"firstName\":\"Bob\",\"lastName\":\"Jones\",\"dateOfBirth\":\"1985-03-20\"}")
                .when()
                .post("/patients")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void get_unknown_patient_returns_404() {
        when(readPatientMock.findById(eq(PatientId.of(PATIENT_ID)), any())).thenThrow(new PatientNotFoundException(PatientId.of(PATIENT_ID)));

        given().when()
                .get("/patients/" + PATIENT_ID)
                .then()
                .statusCode(404)
                .body("status", equalTo(404));
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void clerk_can_list_patients_with_pagination_metadata() {
        var page = new PatientPage(List.of(ALICE), 1, 0, 20);
        when(listPatientMock.listPatients(eq(UserId.of("clerk-1")), any())).thenReturn(page);

        given().when()
                .get("/patients")
                .then()
                .statusCode(200)
                .body("totalElements", equalTo(1))
                .body("page", equalTo(0))
                .body("_embedded.patients[0].firstName", equalTo("Alice"))
                .body("_links.self.href", containsString("/patients"));
    }


    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void register_with_missing_field_returns_400() {
        given().contentType(ContentType.JSON)
                .body("{\"firstName\":\"Alice\",\"lastName\":\"Smith\"}")
                .when()
                .post("/patients")
                .then()
                .statusCode(400);
    }
}
