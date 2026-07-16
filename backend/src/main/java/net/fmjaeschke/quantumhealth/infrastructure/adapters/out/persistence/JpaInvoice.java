package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import net.fmjaeschke.quantumhealth.domain.model.EncounterId;
import net.fmjaeschke.quantumhealth.domain.model.Invoice;
import net.fmjaeschke.quantumhealth.domain.model.InvoiceId;
import net.fmjaeschke.quantumhealth.domain.model.InvoiceStatus;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "qh_invoice")
public class JpaInvoice {

    @Id
    public UUID id;

    @Column(name = "encounter_id", nullable = false)
    public UUID encounterId;

    @Column(name = "patient_id", nullable = false)
    public UUID patientId;

    @Column(name = "total_amount", nullable = false)
    public BigDecimal totalAmount;

    @Column(name = "insurer_amount", nullable = false)
    public BigDecimal insurerAmount;

    @Column(name = "patient_copay", nullable = false)
    public BigDecimal patientCopay;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public InvoiceStatus status;

    @Column(name = "patient_paid_at")
    public Instant patientPaidAt;

    public static JpaInvoice from(Invoice invoice) {
        var entity = new JpaInvoice();
        entity.id             = invoice.getId().value();
        entity.encounterId    = invoice.getEncounterId().value();
        entity.patientId      = invoice.getPatientId().value();
        entity.totalAmount    = invoice.getTotalAmount();
        entity.insurerAmount  = invoice.getInsurerAmount();
        entity.patientCopay   = invoice.getPatientCopay();
        entity.status         = invoice.getStatus();
        entity.patientPaidAt  = invoice.getPatientPaidAt().orElse(null);
        return entity;
    }

    public Invoice toDomain() {
        return Invoice.reconstitute(
                InvoiceId.of(id),
                EncounterId.of(encounterId),
                PatientId.of(patientId),
                totalAmount,
                insurerAmount,
                patientCopay,
                status,
                patientPaidAt);
    }
}
