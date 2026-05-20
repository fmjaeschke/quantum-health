package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import net.fmjaeschke.quantumhealth.application.exception.AppointmentNotFoundException;
import net.fmjaeschke.quantumhealth.application.ports.in.CancelAppointmentUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.CheckInUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ConfirmAppointmentUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.FindDoctorsUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ListAppointmentsUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ReadAppointmentUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ScheduleAppointmentUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.StartEncounterUseCase;
import net.fmjaeschke.quantumhealth.domain.model.Appointment;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentId;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentPage;
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
    static final String REASON = "Annual checkup";

    static final Appointment PENDING = Appointment.reconstitute(
            AppointmentId.of(APPT_ID),
            PatientId.of(PATIENT_UUID),
            "Alice Smith",
            UserId.of("dr-smith"),
            "Dr. Smith",
            TOMORROW,
            REASON,
            AppointmentStatus.PENDING);

    static final Appointment CONFIRMED = Appointment.reconstitute(
            AppointmentId.of(APPT_ID),
            PatientId.of(PATIENT_UUID),
            "Alice Smith",
            UserId.of("dr-smith"),
            "Dr. Smith",
            TOMORROW,
            REASON,
            AppointmentStatus.CONFIRMED);

    static final Appointment ARRIVED = Appointment.reconstitute(
            AppointmentId.of(APPT_ID),
            PatientId.of(PATIENT_UUID),
            "Alice Smith",
            UserId.of("dr-smith"),
            "Dr. Smith",
            TOMORROW,
            REASON,
            AppointmentStatus.ARRIVED);

    static final Appointment IN_PROGRESS = Appointment.reconstitute(
            AppointmentId.of(APPT_ID),
            PatientId.of(PATIENT_UUID),
            "Alice Smith",
            UserId.of("dr-smith"),
            "Dr. Smith",
            TOMORROW,
            REASON,
            AppointmentStatus.IN_PROGRESS);

    @InjectMock ScheduleAppointmentUseCase scheduleMock;
    @InjectMock ReadAppointmentUseCase readMock;
    @InjectMock ListAppointmentsUseCase listMock;
    @InjectMock ConfirmAppointmentUseCase confirmMock;
    @InjectMock CancelAppointmentUseCase cancelMock;
    @InjectMock CheckInUseCase checkInMock;
    @InjectMock StartEncounterUseCase startMock;
    @InjectMock FindDoctorsUseCase findDoctorsMock;

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void clerk_can_schedule_appointment_and_gets_hal_response() {
        when(scheduleMock.schedule(any(), any(), any(), any(), any())).thenReturn(PENDING);

        given().contentType(ContentType.JSON)
                .body("""
                        {"patientId":"%s","doctorId":"dr-smith",
                         "scheduledAt":"%s","reason":"%s"}
                        """.formatted(PATIENT_UUID, TOMORROW, REASON))
                .when()
                .post("/appointments")
                .then()
                .statusCode(201)
                .header("Location", containsString("/appointments/"))
                .body("patientName", equalTo("Alice Smith"))
                .body("reason", equalTo(REASON))
                .body("status", equalTo("PENDING"))
                .body("_links.self.href", containsString("/appointments/" + APPT_ID))
                .body("_links.confirm.href", containsString("/confirm"))
                .body("_links.cancel.href", containsString("/cancel"));
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void schedule_with_past_date_returns_400() {
        var pastDate = LocalDateTime.now().minusDays(1);

        given().contentType(ContentType.JSON)
                .body("""
                        {"patientId":"%s","doctorId":"dr-smith",
                         "scheduledAt":"%s","reason":"%s"}
                        """.formatted(PATIENT_UUID, pastDate, REASON))
                .when()
                .post("/appointments")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "dr-smith", roles = {"DOCTOR"})
    void doctor_cannot_schedule_appointment() {
        given().contentType(ContentType.JSON)
                .body("""
                        {"patientId":"%s","doctorId":"dr-smith",
                         "scheduledAt":"%s","reason":"%s"}
                        """.formatted(PATIENT_UUID, TOMORROW, REASON))
                .when()
                .post("/appointments")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void confirmed_appointment_has_check_in_link_for_clerk_but_no_confirm_link() {
        when(readMock.findById(eq(AppointmentId.of(APPT_ID)), any())).thenReturn(CONFIRMED);

        given().when()
                .get("/appointments/" + APPT_ID)
                .then()
                .statusCode(200)
                .body("status", equalTo("CONFIRMED"))
                .body("reason", equalTo(REASON))
                .body("_links.cancel.href", containsString("/cancel"))
                .body("_links.'check-in'.href", containsString("/check-in"))
                .body("_links", not(hasKey("confirm")))
                .body("_links", not(hasKey("start")));
    }

    @Test
    @TestSecurity(user = "dr-smith", roles = {"DOCTOR"})
    void confirmed_appointment_has_start_link_for_doctor() {
        when(readMock.findById(eq(AppointmentId.of(APPT_ID)), any())).thenReturn(CONFIRMED);

        given().when()
                .get("/appointments/" + APPT_ID)
                .then()
                .statusCode(200)
                .body("_links.start.href", containsString("/start"))
                .body("_links", not(hasKey("check-in")))
                .body("_links", not(hasKey("confirm")));
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void clerk_can_list_all_appointments() {
        when(listMock.list(any(), any())).thenReturn(new AppointmentPage(List.of(PENDING), 1, 0, 20));

        given().when()
                .get("/appointments")
                .then()
                .statusCode(200)
                .body("_embedded.appointments", hasSize(1))
                .body("_embedded.appointments[0].patientName", equalTo("Alice Smith"))
                .body("_links.self.href", containsString("/appointments"))
                .body("page", equalTo(0))
                .body("pageSize", equalTo(20))
                .body("totalElements", equalTo(1));
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void list_with_status_filter_returns_only_matching_appointments() {
        when(listMock.list(any(), any())).thenReturn(new AppointmentPage(List.of(PENDING), 1, 0, 20));

        given().queryParam("status", "PENDING").when()
                .get("/appointments")
                .then()
                .statusCode(200)
                .body("_embedded.appointments", hasSize(1))
                .body("_embedded.appointments[0].status", equalTo("PENDING"));
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void list_with_page_and_size_returns_pagination_metadata() {
        when(listMock.list(any(), any())).thenReturn(new AppointmentPage(List.of(PENDING), 10, 1, 5));

        given().queryParam("page", 1).queryParam("size", 5).when()
                .get("/appointments")
                .then()
                .statusCode(200)
                .body("page", equalTo(1))
                .body("pageSize", equalTo(5))
                .body("totalElements", equalTo(10));
    }

    @Test
    @TestSecurity(user = "dr-smith", roles = {"DOCTOR"})
    void doctor_lists_only_own_appointments() {
        when(listMock.list(any(), any())).thenReturn(new AppointmentPage(List.of(PENDING), 1, 0, 20));

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
                "Alice Smith", UserId.of("dr-smith"), "Dr. Smith", TOMORROW, REASON, AppointmentStatus.CANCELLED);
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

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void clerk_can_check_in_confirmed_appointment() {
        when(checkInMock.checkIn(eq(AppointmentId.of(APPT_ID)), any())).thenReturn(ARRIVED);

        given().when()
                .post("/appointments/" + APPT_ID + "/check-in")
                .then()
                .statusCode(200)
                .body("status", equalTo("ARRIVED"))
                .body("_links", not(hasKey("check-in")))
                .body("_links", not(hasKey("start")));
    }

    @Test
    @TestSecurity(user = "nurse-1", roles = {"NURSE"})
    void nurse_cannot_check_in() {
        given().when()
                .post("/appointments/" + APPT_ID + "/check-in")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "dr-smith", roles = {"DOCTOR"})
    void doctor_can_start_confirmed_appointment() {
        when(startMock.start(eq(AppointmentId.of(APPT_ID)), any())).thenReturn(IN_PROGRESS);

        given().when()
                .post("/appointments/" + APPT_ID + "/start")
                .then()
                .statusCode(200)
                .body("status", equalTo("IN_PROGRESS"))
                .body("_links", not(hasKey("start")))
                .body("_links", not(hasKey("cancel")));
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void clerk_cannot_start_appointment() {
        given().when()
                .post("/appointments/" + APPT_ID + "/start")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void invalid_transition_returns_409() {
        when(confirmMock.confirm(eq(AppointmentId.of(APPT_ID)), any()))
                .thenThrow(new IllegalStateException("Can only confirm PENDING appointments, current status: CONFIRMED"));

        given().when()
                .post("/appointments/" + APPT_ID + "/confirm")
                .then()
                .statusCode(409);
    }

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void pending_appointment_has_confirm_link_for_clerk() {
        when(readMock.findById(eq(AppointmentId.of(APPT_ID)), any())).thenReturn(PENDING);

        given().when()
                .get("/appointments/" + APPT_ID)
                .then()
                .statusCode(200)
                .body("_links.confirm.href", containsString("/confirm"))
                .body("_links.cancel.href", containsString("/cancel"))
                .body("_links", not(hasKey("check-in")))
                .body("_links", not(hasKey("start")));
    }

    @Test
    @TestSecurity(user = "nurse-1", roles = {"NURSE"})
    void nurse_sees_no_action_links_on_pending_appointment() {
        when(readMock.findById(eq(AppointmentId.of(APPT_ID)), any())).thenReturn(PENDING);

        given().when()
                .get("/appointments/" + APPT_ID)
                .then()
                .statusCode(200)
                .body("_links", not(hasKey("confirm")))
                .body("_links", not(hasKey("check-in")))
                .body("_links", not(hasKey("start")))
                .body("_links", not(hasKey("cancel")));
    }
}
