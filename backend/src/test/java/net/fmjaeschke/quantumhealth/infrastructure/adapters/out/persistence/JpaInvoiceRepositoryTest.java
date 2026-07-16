package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import net.fmjaeschke.quantumhealth.application.ports.out.EncounterRepository;
import net.fmjaeschke.quantumhealth.application.ports.out.InvoiceRepository;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentId;
import net.fmjaeschke.quantumhealth.domain.model.Encounter;
import net.fmjaeschke.quantumhealth.domain.model.EncounterId;
import net.fmjaeschke.quantumhealth.domain.model.Invoice;
import net.fmjaeschke.quantumhealth.domain.model.InvoiceId;
import net.fmjaeschke.quantumhealth.domain.model.InvoiceStatus;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class JpaInvoiceRepositoryTest {

    @Inject
    InvoiceRepository repository;

    @Inject
    EncounterRepository encounterRepository;

    private EncounterId persistedEncounterId() {
        var encounter = encounterRepository.saveNew(
                Encounter.create(AppointmentId.generate(), UserId.of("doctor-1"), PatientId.generate()));
        return encounter.getId();
    }

    @Test
    @Transactional
    void saveNew_persists_invoice_and_findById_returns_it() {
        var encounterId = persistedEncounterId();
        var patientId = PatientId.generate();

        var saved = repository.saveNew(Invoice.draft(encounterId, patientId, new BigDecimal("150.00")));

        assertThat(saved.getId()).isNotNull();
        var retrieved = repository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getEncounterId()).isEqualTo(encounterId);
        assertThat(retrieved.get().getPatientId()).isEqualTo(patientId);
        assertThat(retrieved.get().getTotalAmount()).isEqualByComparingTo("150.00");
        assertThat(retrieved.get().getStatus()).isEqualTo(InvoiceStatus.DRAFT);
    }

    @Test
    @Transactional
    void findById_returns_empty_when_not_found() {
        var found = repository.findById(InvoiceId.generate());

        assertThat(found).isEmpty();
    }

    @Test
    @Transactional
    void findByEncounterId_returns_linked_invoice() {
        var encounterId = persistedEncounterId();
        var saved = repository.saveNew(Invoice.draft(encounterId, PatientId.generate(), new BigDecimal("150.00")));

        var found = repository.findByEncounterId(encounterId);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    @Transactional
    void findByEncounterId_returns_empty_when_not_found() {
        var found = repository.findByEncounterId(EncounterId.generate());

        assertThat(found).isEmpty();
    }

    @Test
    @Transactional
    void save_persists_status_transitions() {
        var saved = repository.saveNew(Invoice.draft(persistedEncounterId(), PatientId.generate(), new BigDecimal("150.00")));

        repository.save(saved.submit());
        var afterSubmit = repository.findById(saved.getId());
        assertThat(afterSubmit).isPresent();
        assertThat(afterSubmit.get().getStatus()).isEqualTo(InvoiceStatus.SUBMITTED);

        repository.save(afterSubmit.get().pay());
        var afterPay = repository.findById(saved.getId());
        assertThat(afterPay).isPresent();
        assertThat(afterPay.get().getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    @Transactional
    void save_persists_patientPaidAt() {
        var saved = repository.saveNew(Invoice.draft(persistedEncounterId(), PatientId.generate(), new BigDecimal("150.00")));
        var paid = saved.processPatientPayment();

        repository.save(paid);
        var retrieved = repository.findById(saved.getId());

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getPatientPaidAt()).isPresent();
    }
}
