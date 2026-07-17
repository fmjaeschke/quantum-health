package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import net.fmjaeschke.quantumhealth.application.exception.AccessDeniedException;
import net.fmjaeschke.quantumhealth.application.exception.EncounterNotFoundException;
import net.fmjaeschke.quantumhealth.application.ports.in.AddClinicalNoteUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.CompleteEncounterUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ReadEncounterUseCase;
import net.fmjaeschke.quantumhealth.domain.exception.EncounterCompletedException;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentId;
import net.fmjaeschke.quantumhealth.domain.model.Encounter;
import net.fmjaeschke.quantumhealth.domain.model.EncounterId;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
class EncounterResourceTest {

    static final UUID ENCOUNTER_UUID = UUID.fromString("dddddddd-0000-0000-0000-000000000001");
    static final UUID APPOINTMENT_UUID = UUID.fromString("eeeeeeee-0000-0000-0000-000000000001");
    static final UUID PATIENT_UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    static final Encounter ENCOUNTER = Encounter.reconstitute(
            EncounterId.of(ENCOUNTER_UUID),
            AppointmentId.of(APPOINTMENT_UUID),
            UserId.of("dr-smith"),
            PatientId.of(PATIENT_UUID),
            null,
            List.of());

    @InjectMock ReadEncounterUseCase readMock;
    @InjectMock AddClinicalNoteUseCase addNoteMock;
    @InjectMock CompleteEncounterUseCase completeMock;

    @Test
    @TestSecurity(user = "dr-smith", roles = {"DOCTOR"})
    void assigned_doctor_can_read_encounter() {
        when(readMock.findById(any(), any())).thenReturn(ENCOUNTER);

        given()
                .when()
                .get("/encounters/" + ENCOUNTER_UUID)
                .then()
                .statusCode(200)
                .body("patientId", equalTo(PATIENT_UUID.toString()))
                .body("doctorId", equalTo("dr-smith"))
                .body("_links.self.href", containsString("/encounters/" + ENCOUNTER_UUID));
    }

    @Test
    @TestSecurity(user = "dr-other", roles = {"DOCTOR"})
    void unrelated_doctor_gets_404() {
        when(readMock.findById(any(), any())).thenThrow(new EncounterNotFoundException(EncounterId.of(ENCOUNTER_UUID)));

        given()
                .when()
                .get("/encounters/" + ENCOUNTER_UUID)
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "dr-smith", roles = {"DOCTOR"})
    void assigned_doctor_can_add_note() {
        var withNote = ENCOUNTER.addNote("Patient presents with mild fever.", UserId.of("dr-smith"), Instant.now());
        when(addNoteMock.addNote(any(), any(), any())).thenReturn(withNote);

        given()
                .contentType("application/json")
                .body("{\"content\":\"Patient presents with mild fever.\"}")
                .when()
                .post("/encounters/" + ENCOUNTER_UUID + "/notes")
                .then()
                .statusCode(200)
                .body("latestNoteContent", equalTo("Patient presents with mild fever."));
    }

    @Test
    @TestSecurity(user = "dr-other", roles = {"DOCTOR"})
    void non_assigned_doctor_gets_403_adding_note() {
        when(addNoteMock.addNote(any(), any(), any()))
                .thenThrow(new AccessDeniedException("write encounter note " + ENCOUNTER_UUID));

        given()
                .contentType("application/json")
                .body("{\"content\":\"Sneaky note.\"}")
                .when()
                .post("/encounters/" + ENCOUNTER_UUID + "/notes")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "nurse-1", roles = {"NURSE"})
    void nurse_gets_403_adding_note() {
        given()
                .contentType("application/json")
                .body("{\"content\":\"Sneaky note.\"}")
                .when()
                .post("/encounters/" + ENCOUNTER_UUID + "/notes")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void clerk_gets_403_adding_note() {
        given()
                .contentType("application/json")
                .body("{\"content\":\"Sneaky note.\"}")
                .when()
                .post("/encounters/" + ENCOUNTER_UUID + "/notes")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "dr-smith", roles = {"DOCTOR"})
    void get_encounter_reflects_latest_note_content() {
        var withNote = ENCOUNTER.addNote("First note.", UserId.of("dr-smith"), Instant.now())
                .addNote("Second note.", UserId.of("dr-smith"), Instant.now());
        when(readMock.findById(any(), any())).thenReturn(withNote);

        given()
                .when()
                .get("/encounters/" + ENCOUNTER_UUID)
                .then()
                .statusCode(200)
                .body("latestNoteContent", equalTo("Second note."));
    }

    @Test
    @TestSecurity(user = "dr-smith", roles = {"DOCTOR"})
    void adding_note_to_completed_encounter_returns_409() {
        when(addNoteMock.addNote(any(), any(), any()))
                .thenThrow(new EncounterCompletedException(EncounterId.of(ENCOUNTER_UUID)));

        given()
                .contentType("application/json")
                .body("{\"content\":\"Too late.\"}")
                .when()
                .post("/encounters/" + ENCOUNTER_UUID + "/notes")
                .then()
                .statusCode(409);
    }

    @Test
    @TestSecurity(user = "dr-smith", roles = {"DOCTOR"})
    void assigned_doctor_can_complete_encounter() {
        var completed = Encounter.reconstitute(EncounterId.of(ENCOUNTER_UUID), AppointmentId.of(APPOINTMENT_UUID),
                UserId.of("dr-smith"), PatientId.of(PATIENT_UUID), Instant.now(), List.of());
        when(completeMock.complete(any(), any())).thenReturn(completed);

        given()
                .when()
                .post("/encounters/" + ENCOUNTER_UUID + "/complete")
                .then()
                .statusCode(200)
                .body("completedAt", notNullValue())
                .body("_links", not(hasKey("complete-encounter")))
                .body("_links", not(hasKey("add-clinical-note")));
    }

    @Test
    @TestSecurity(user = "dr-other", roles = {"DOCTOR"})
    void non_assigned_doctor_gets_403_completing_encounter() {
        when(completeMock.complete(any(), any()))
                .thenThrow(new AccessDeniedException("complete encounter " + ENCOUNTER_UUID));

        given()
                .when()
                .post("/encounters/" + ENCOUNTER_UUID + "/complete")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "nurse-1", roles = {"NURSE"})
    void nurse_gets_403_completing_encounter() {
        given()
                .when()
                .post("/encounters/" + ENCOUNTER_UUID + "/complete")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void clerk_gets_403_completing_encounter() {
        given()
                .when()
                .post("/encounters/" + ENCOUNTER_UUID + "/complete")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "dr-smith", roles = {"DOCTOR"})
    void incomplete_encounter_has_complete_encounter_link_for_assigned_doctor() {
        when(readMock.findById(any(), any())).thenReturn(ENCOUNTER);

        given()
                .when()
                .get("/encounters/" + ENCOUNTER_UUID)
                .then()
                .statusCode(200)
                .body("_links.'complete-encounter'.href", containsString("/complete"));
    }

    @Test
    @TestSecurity(user = "dr-other", roles = {"DOCTOR"})
    void incomplete_encounter_has_no_complete_encounter_link_for_non_assigned_doctor() {
        when(readMock.findById(any(), any())).thenReturn(ENCOUNTER);

        given()
                .when()
                .get("/encounters/" + ENCOUNTER_UUID)
                .then()
                .statusCode(200)
                .body("_links", not(hasKey("complete-encounter")));
    }
}
