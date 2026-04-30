package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import net.fmjaeschke.quantumhealth.application.exception.AppointmentNotFoundException;
import net.fmjaeschke.quantumhealth.application.ports.in.CancelAppointmentUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ConfirmAppointmentUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ListAppointmentsUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ReadAppointmentUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ScheduleAppointmentUseCase;
import net.fmjaeschke.quantumhealth.domain.model.Appointment;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentId;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentStatus;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@QuarkusTest
class AppointmentResourceTest {

    static final UUID APPT_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");
    static final UUID PATIENT_UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    static final LocalDateTime TOMORROW = LocalDateTime.now().plusDays(1);

    static final Appointment SCHEDULED = Appointment.reconstitute(
            AppointmentId.of(APPT_ID),
            PatientId.of(PATIENT_UUID),
            "Alice Smith",
            UserId.of("dr-smith"),
            "Dr. Smith",
            TOMORROW,
            AppointmentStatus.SCHEDULED);

    static final Appointment CONFIRMED = Appointment.reconstitute(
            AppointmentId.of(APPT_ID),
            PatientId.of(PATIENT_UUID),
            "Alice Smith",
            UserId.of("dr-smith"),
            "Dr. Smith",
            TOMORROW,
            AppointmentStatus.CONFIRMED);

    @InjectMock ScheduleAppointmentUseCase scheduleMock;
    @InjectMock ReadAppointmentUseCase readMock;
    @InjectMock ListAppointmentsUseCase listMock;
    @InjectMock ConfirmAppointmentUseCase confirmMock;
    @InjectMock CancelAppointmentUseCase cancelMock;

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void clerk_can_schedule_appointment_and_gets_hal_response() {
        when(scheduleMock.schedule(any(), any(), any(), any(), any(), any())).thenReturn(SCHEDULED);

        given().contentType(ContentType.JSON)
                .body("""
                        {"patientId":"%s","patientName":"Alice Smith",
                         "doctorId":"dr-smith","doctorName":"Dr. Smith",
                         "scheduledAt":"%s"}
                        """.formatted(PATIENT_UUID, TOMORROW))
                .when()
                .post("/appointments")
                .then()
                .statusCode(201)
                .header("Location", containsString("/appointments/"))
                .body("patientName", equalTo("Alice Smith"))
                .body("status", equalTo("SCHEDULED"))
                .body("_links.self.href", containsString("/appointments/" + APPT_ID))
                .body("_links.confirm.href", containsString("/confirm"))
                .body("_links.cancel.href", containsString("/cancel"));
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void confirmed_appointment_has_no_confirm_link() {
        when(readMock.findById(eq(AppointmentId.of(APPT_ID)), any())).thenReturn(CONFIRMED);

        given().when()
                .get("/appointments/" + APPT_ID)
                .then()
                .statusCode(200)
                .body("status", equalTo("CONFIRMED"))
                .body("_links.cancel.href", containsString("/cancel"))
                .body("_links", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasKey("confirm")));
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void clerk_can_list_all_appointments() {
        when(listMock.findAll(any())).thenReturn(List.of(SCHEDULED));

        given().when()
                .get("/appointments")
                .then()
                .statusCode(200)
                .body("_embedded.appointments", hasSize(1))
                .body("_embedded.appointments[0].patientName", equalTo("Alice Smith"))
                .body("_links.self.href", containsString("/appointments"));
    }

    @Test
    @TestSecurity(user = "dr-smith", roles = {"DOCTOR"})
    void doctor_lists_only_own_appointments() {
        when(listMock.listByDoctor(eq(UserId.of("dr-smith")), any())).thenReturn(List.of(SCHEDULED));

        given().when()
                .get("/appointments")
                .then()
                .statusCode(200)
                .body("_embedded.appointments", hasSize(1));
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void confirm_returns_confirmed_appointment_with_cancel_link() {
        when(confirmMock.confirm(eq(AppointmentId.of(APPT_ID)), any())).thenReturn(CONFIRMED);

        given().when()
                .post("/appointments/" + APPT_ID + "/confirm")
                .then()
                .statusCode(200)
                .body("status", equalTo("CONFIRMED"))
                .body("_links.cancel.href", containsString("/cancel"));
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void cancel_returns_cancelled_appointment_with_no_transition_links() {
        var cancelled = Appointment.reconstitute(AppointmentId.of(APPT_ID), PatientId.of(PATIENT_UUID),
                "Alice Smith", UserId.of("dr-smith"), "Dr. Smith", TOMORROW, AppointmentStatus.CANCELLED);
        when(cancelMock.cancel(eq(AppointmentId.of(APPT_ID)), any())).thenReturn(cancelled);

        given().when()
                .post("/appointments/" + APPT_ID + "/cancel")
                .then()
                .statusCode(200)
                .body("status", equalTo("CANCELLED"))
                .body("_links", not(hasKey("confirm")))
                .body("_links", not(hasKey("cancel")));
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void get_unknown_appointment_returns_404() {
        when(readMock.findById(eq(AppointmentId.of(APPT_ID)), any()))
                .thenThrow(new AppointmentNotFoundException(AppointmentId.of(APPT_ID)));

        given().when()
                .get("/appointments/" + APPT_ID)
                .then()
                .statusCode(404)
                .body("status", equalTo(404));
    }

    @Test
    void unauthenticated_request_is_rejected() {
        given().contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/appointments")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void schedule_with_missing_field_returns_400() {
        given().contentType(ContentType.JSON)
                .body("{\"patientId\":\"" + PATIENT_UUID + "\"}")
                .when()
                .post("/appointments")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "nurse-1", roles = {"NURSE"})
    void nurse_cannot_schedule_appointment() {
        given().contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/appointments")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "nurse-1", roles = {"NURSE"})
    void nurse_cannot_confirm_appointment() {
        given().when()
                .post("/appointments/" + APPT_ID + "/confirm")
                .then()
                .statusCode(403);
    }
}
