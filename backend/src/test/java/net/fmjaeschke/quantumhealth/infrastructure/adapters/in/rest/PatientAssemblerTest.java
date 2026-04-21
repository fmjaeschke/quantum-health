package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest;

import io.quarkus.hal.HalEntityWrapper;
import io.quarkus.hal.HalService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import net.fmjaeschke.quantumhealth.domain.model.Patient;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.PatientResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class PatientAssemblerTest {

    @Inject
    PatientAssembler assembler;

    @InjectMock
    HalService halServiceMock;

    @Test
    void toHal_maps_patient_fields_to_dto() {
        var id = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        var patient = Patient.reconstitute(PatientId.of(id), "Alice", "Smith", LocalDate.of(1990, 5, 15));

        when(halServiceMock.toHalWrapper(any(PatientResponse.class))).thenAnswer(inv -> new HalEntityWrapper<>(inv.getArgument(0)));

        HalEntityWrapper<PatientResponse> hal = assembler.toHal(patient);
        var dto = hal.getEntity();

        var captor = ArgumentCaptor.forClass(PatientResponse.class);
        verify(halServiceMock).toHalWrapper(captor.capture());

        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.firstName()).isEqualTo("Alice");
        assertThat(dto.lastName()).isEqualTo("Smith");
        assertThat(dto.dateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 15));
    }
}
