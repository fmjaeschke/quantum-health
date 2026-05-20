package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import net.fmjaeschke.quantumhealth.application.ports.in.FindDoctorsUseCase;
import net.fmjaeschke.quantumhealth.domain.model.Doctor;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;

@QuarkusTest
class DoctorResourceTest {

    @InjectMock
    FindDoctorsUseCase findDoctorsMock;

    @Test
    @TestSecurity(user = "clerk-1", roles = {"CLERK"})
    void clerk_gets_list_of_doctors() {
        when(findDoctorsMock.findDoctors()).thenReturn(List.of(
                new Doctor(UserId.of("dr-uuid-1"), "Dr. Alice Smith"),
                new Doctor(UserId.of("dr-uuid-2"), "Dr. Bob Jones")
        ));

        given().when()
                .get("/doctors")
                .then()
                .statusCode(200)
                .body("$", hasSize(2))
                .body("[0].id", equalTo("dr-uuid-1"))
                .body("[0].displayName", equalTo("Dr. Alice Smith"))
                .body("[1].id", equalTo("dr-uuid-2"))
                .body("[1].displayName", equalTo("Dr. Bob Jones"));
    }

    @Test
    @TestSecurity(user = "nurse-1", roles = {"NURSE"})
    void nurse_gets_403_on_doctors_endpoint() {
        given().when()
                .get("/doctors")
                .then()
                .statusCode(403);
    }

}
