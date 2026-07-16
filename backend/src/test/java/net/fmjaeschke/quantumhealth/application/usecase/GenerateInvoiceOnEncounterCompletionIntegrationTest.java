package net.fmjaeschke.quantumhealth.application.usecase;

import com.github.database.rider.cdi.api.DBRider;
import com.github.database.rider.core.api.dataset.DataSet;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import net.fmjaeschke.quantumhealth.application.ports.in.CompleteEncounterUseCase;
import net.fmjaeschke.quantumhealth.application.ports.out.AppointmentRepository;
import net.fmjaeschke.quantumhealth.application.ports.out.EncounterRepository;
import net.fmjaeschke.quantumhealth.application.ports.out.InvoiceRepository;
import net.fmjaeschke.quantumhealth.domain.model.Appointment;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentId;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentStatus;
import net.fmjaeschke.quantumhealth.domain.model.Encounter;
import net.fmjaeschke.quantumhealth.domain.model.InvoiceStatus;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the real cross-context trigger introduced by issue 037: completing an encounter
 * fires {@code EncounterCompletedEvent} through the real CDI event bus, which a production
 * listener observes and turns into an auto-submitted invoice. No mocks — every collaborator
 * here (repositories, event publisher, listener, InvoiceService) is the real, wired bean.
 */
@QuarkusTest
@DBRider
class GenerateInvoiceOnEncounterCompletionIntegrationTest {

    private static final UUID GOLD_PATIENT = UUID.fromString("11111111-0000-0000-0000-000000000001");
    private static final UUID SILVER_PATIENT = UUID.fromString("11111111-0000-0000-0000-000000000002");
    private static final UUID BRONZE_PATIENT = UUID.fromString("11111111-0000-0000-0000-000000000003");
    private static final UUID UNINSURED_PATIENT = UUID.fromString("11111111-0000-0000-0000-000000000004");

    @Inject
    CompleteEncounterUseCase completeEncounter;

    @Inject
    AppointmentRepository appointmentRepository;

    @Inject
    EncounterRepository encounterRepository;

    @Inject
    InvoiceRepository invoiceRepository;

    private Encounter inProgressEncounterFor(UUID patientUuid, UserId doctorId) {
        var patientId = PatientId.of(patientUuid);
        var appointment = appointmentRepository.saveNew(Appointment.reconstitute(
                AppointmentId.generate(), patientId, "Patient",
                doctorId, "Dr. Billing", Instant.now(), "checkup", AppointmentStatus.IN_PROGRESS));
        return encounterRepository.saveNew(Encounter.create(appointment.getId(), doctorId, patientId));
    }

    @Test
    @DataSet("datasets/billing.yml")
    @Transactional
    void completing_encounter_for_gold_patient_generates_submitted_invoice_with_90_percent_coverage() {
        var doctorId = UserId.of("dr-" + UUID.randomUUID());
        var encounter = inProgressEncounterFor(GOLD_PATIENT, doctorId);

        completeEncounter.complete(encounter.getId(), doctorId);

        var invoice = invoiceRepository.findByEncounterId(encounter.getId()).orElseThrow();
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.SUBMITTED);
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo("150.00");
        assertThat(invoice.getInsurerAmount()).isEqualByComparingTo("135.00");
        assertThat(invoice.getPatientCopay()).isEqualByComparingTo("15.00");
    }

    @Test
    @DataSet("datasets/billing.yml")
    @Transactional
    void completing_encounter_for_silver_patient_generates_submitted_invoice_with_70_percent_coverage() {
        var doctorId = UserId.of("dr-" + UUID.randomUUID());
        var encounter = inProgressEncounterFor(SILVER_PATIENT, doctorId);

        completeEncounter.complete(encounter.getId(), doctorId);

        var invoice = invoiceRepository.findByEncounterId(encounter.getId()).orElseThrow();
        assertThat(invoice.getInsurerAmount()).isEqualByComparingTo("105.00");
        assertThat(invoice.getPatientCopay()).isEqualByComparingTo("45.00");
    }

    @Test
    @DataSet("datasets/billing.yml")
    @Transactional
    void completing_encounter_for_bronze_patient_generates_submitted_invoice_with_50_percent_coverage() {
        var doctorId = UserId.of("dr-" + UUID.randomUUID());
        var encounter = inProgressEncounterFor(BRONZE_PATIENT, doctorId);

        completeEncounter.complete(encounter.getId(), doctorId);

        var invoice = invoiceRepository.findByEncounterId(encounter.getId()).orElseThrow();
        assertThat(invoice.getInsurerAmount()).isEqualByComparingTo("75.00");
        assertThat(invoice.getPatientCopay()).isEqualByComparingTo("75.00");
    }

    @Test
    @DataSet("datasets/billing.yml")
    @Transactional
    void completing_encounter_for_uninsured_patient_generates_submitted_invoice_with_full_copay() {
        var doctorId = UserId.of("dr-" + UUID.randomUUID());
        var encounter = inProgressEncounterFor(UNINSURED_PATIENT, doctorId);

        completeEncounter.complete(encounter.getId(), doctorId);

        var invoice = invoiceRepository.findByEncounterId(encounter.getId()).orElseThrow();
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.SUBMITTED);
        assertThat(invoice.getInsurerAmount()).isEqualByComparingTo("0.00");
        assertThat(invoice.getPatientCopay()).isEqualByComparingTo("150.00");
    }

    @Test
    @DataSet("datasets/billing.yml")
    @Transactional
    void completing_encounter_creates_exactly_one_invoice() {
        var doctorId = UserId.of("dr-" + UUID.randomUUID());
        var encounter = inProgressEncounterFor(GOLD_PATIENT, doctorId);

        completeEncounter.complete(encounter.getId(), doctorId);

        assertThat(invoiceRepository.findByEncounterId(encounter.getId())).isPresent();
    }
}
