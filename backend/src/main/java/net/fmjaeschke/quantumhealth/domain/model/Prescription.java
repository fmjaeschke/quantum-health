package net.fmjaeschke.quantumhealth.domain.model;

import net.fmjaeschke.quantumhealth.domain.exception.InvalidPrescriptionStateException;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class Prescription {

    private final PrescriptionId id;
    private final PatientId patientId;
    private final String patientName;
    private final UserId doctorId;
    private final String doctorName;
    private final List<MedicationItem> medications;
    private final Instant issuedAt;
    private final Disposition disposition;
    private final Long version;

    private Prescription(PrescriptionId id, PatientId patientId, String patientName,
                         UserId doctorId, String doctorName, List<MedicationItem> medications,
                         Instant issuedAt, Disposition disposition, Long version) {
        this.id          = Objects.requireNonNull(id,          "id");
        this.patientId   = Objects.requireNonNull(patientId,   "patientId");
        this.patientName = Objects.requireNonNull(patientName, "patientName");
        this.doctorId    = Objects.requireNonNull(doctorId,    "doctorId");
        this.doctorName  = Objects.requireNonNull(doctorName,  "doctorName");
        this.medications = List.copyOf(Objects.requireNonNull(medications, "medications"));
        this.issuedAt    = Objects.requireNonNull(issuedAt,    "issuedAt");
        this.disposition = Objects.requireNonNull(disposition, "disposition");
        this.version     = version;
    }

    public static Prescription issue(PatientId patientId, String patientName,
                                     UserId doctorId, String doctorName,
                                     List<MedicationItem> medications, Instant issuedAt) {
        return new Prescription(PrescriptionId.generate(), patientId, patientName,
                doctorId, doctorName, medications, issuedAt, Disposition.issued(), null);
    }

    public static Prescription reconstitute(PrescriptionId id, PatientId patientId, String patientName,
                                            UserId doctorId, String doctorName, List<MedicationItem> medications,
                                            Instant issuedAt, Disposition disposition, Long version) {
        return new Prescription(id, patientId, patientName, doctorId, doctorName, medications,
                issuedAt, disposition, version);
    }

    public Prescription fulfill(UserId actor, Instant at) {
        if (!isFulfillable()) {
            throw new InvalidPrescriptionStateException("fulfill", disposition.status);
        }
        return new Prescription(id, patientId, patientName, doctorId, doctorName, medications,
                issuedAt, Disposition.fulfilled(actor, at), version);
    }

    public Prescription cancel(UserId actor, String reason, Instant at) {
        if (!isCancellable()) {
            throw new InvalidPrescriptionStateException("cancel", disposition.status);
        }
        return new Prescription(id, patientId, patientName, doctorId, doctorName, medications,
                issuedAt, Disposition.cancelled(actor, reason, at), version);
    }

    public Prescription expire(Instant at) {
        if (!isExpirable()) {
            throw new InvalidPrescriptionStateException("expire", disposition.status);
        }
        return new Prescription(id, patientId, patientName, doctorId, doctorName, medications,
                issuedAt, Disposition.expired(at), version);
    }

    public boolean isFulfillable() { return disposition.status == PrescriptionStatus.ISSUED; }
    public boolean isCancellable() { return disposition.status == PrescriptionStatus.ISSUED; }
    public boolean isExpirable()   { return disposition.status == PrescriptionStatus.ISSUED; }

    public PrescriptionId getId()                 { return id; }
    public PatientId getPatientId()               { return patientId; }
    public String getPatientName()                { return patientName; }
    public UserId getDoctorId()                   { return doctorId; }
    public String getDoctorName()                 { return doctorName; }
    public List<MedicationItem> getMedications()  { return medications; }
    public PrescriptionStatus getStatus()         { return disposition.status; }
    public Instant getIssuedAt()                  { return issuedAt; }
    public Instant getFulfilledAt()               { return disposition.fulfilledAt; }
    public UserId getFulfilledBy()                { return disposition.fulfilledBy; }
    public Instant getCancelledAt()               { return disposition.cancelledAt; }
    public UserId getCancelledBy()                { return disposition.cancelledBy; }
    public String getCancelledReason()            { return disposition.cancelledReason; }
    public Instant getExpiredAt()                 { return disposition.expiredAt; }
    public Long getVersion()                      { return version; }
}
