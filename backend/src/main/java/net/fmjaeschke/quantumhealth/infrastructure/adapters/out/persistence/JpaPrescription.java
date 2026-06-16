package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import net.fmjaeschke.quantumhealth.domain.model.Disposition;
import net.fmjaeschke.quantumhealth.domain.model.MedicationItem;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.Prescription;
import net.fmjaeschke.quantumhealth.domain.model.PrescriptionId;
import net.fmjaeschke.quantumhealth.domain.model.PrescriptionStatus;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "qh_prescription")
public class JpaPrescription {

    @Id
    public UUID id;

    @Column(name = "patient_id", nullable = false)
    public UUID patientId;

    @Column(name = "patient_name", nullable = false)
    public String patientName;

    @Column(name = "doctor_id", nullable = false)
    public String doctorId;

    @Column(name = "doctor_name", nullable = false)
    public String doctorName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "medications", columnDefinition = "jsonb", nullable = false)
    public List<MedicationItem> medications;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public PrescriptionStatus status;

    @Column(name = "issued_at", nullable = false)
    public Instant issuedAt;

    @Column(name = "fulfilled_at")
    public Instant fulfilledAt;

    @Column(name = "fulfilled_by")
    public String fulfilledBy;

    @Column(name = "cancelled_at")
    public Instant cancelledAt;

    @Column(name = "cancelled_by")
    public String cancelledBy;

    @Column(name = "cancelled_reason", length = 500)
    public String cancelledReason;

    @Column(name = "expired_at")
    public Instant expiredAt;

    @Version
    @Column(name = "version", nullable = false)
    public Long version;

    public static JpaPrescription from(Prescription p) {
        var entity = new JpaPrescription();
        entity.id             = p.getId().value();
        entity.patientId      = p.getPatientId().value();
        entity.patientName    = p.getPatientName();
        entity.doctorId       = p.getDoctorId().value();
        entity.doctorName     = p.getDoctorName();
        entity.medications    = p.getMedications();
        entity.status         = p.getStatus();
        entity.issuedAt       = p.getIssuedAt();
        entity.fulfilledAt    = p.getFulfilledAt();
        entity.fulfilledBy    = p.getFulfilledBy() != null ? p.getFulfilledBy().value() : null;
        entity.cancelledAt    = p.getCancelledAt();
        entity.cancelledBy    = p.getCancelledBy() != null ? p.getCancelledBy().value() : null;
        entity.cancelledReason = p.getCancelledReason();
        entity.expiredAt      = p.getExpiredAt();
        entity.version        = p.getVersion();
        return entity;
    }

    public Prescription toDomain() {
        return Prescription.reconstitute(
                PrescriptionId.of(id),
                PatientId.of(patientId),
                patientName,
                UserId.of(doctorId),
                doctorName,
                medications,
                issuedAt,
                Disposition.reconstituted(
                        status,
                        fulfilledAt,
                        fulfilledBy != null ? UserId.of(fulfilledBy) : null,
                        cancelledAt,
                        cancelledBy != null ? UserId.of(cancelledBy) : null,
                        cancelledReason,
                        expiredAt),
                version);
    }
}
