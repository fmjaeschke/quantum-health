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
    private final PrescriptionStatus status;
    private final Instant issuedAt;
    private final Instant fulfilledAt;
    private final UserId fulfilledBy;
    private final Instant cancelledAt;
    private final UserId cancelledBy;
    private final String cancelledReason;
    private final Instant expiredAt;
    private final Long version;

    @SuppressWarnings("java:S107")
    private Prescription(PrescriptionId id, PatientId patientId, String patientName,
                         UserId doctorId, String doctorName, List<MedicationItem> medications,
                         PrescriptionStatus status, Instant issuedAt,
                         Instant fulfilledAt, UserId fulfilledBy,
                         Instant cancelledAt, UserId cancelledBy, String cancelledReason,
                         Instant expiredAt, Long version) {
        this.id             = Objects.requireNonNull(id,          "id");
        this.patientId      = Objects.requireNonNull(patientId,   "patientId");
        this.patientName    = Objects.requireNonNull(patientName, "patientName");
        this.doctorId       = Objects.requireNonNull(doctorId,    "doctorId");
        this.doctorName     = Objects.requireNonNull(doctorName,  "doctorName");
        this.medications    = List.copyOf(Objects.requireNonNull(medications, "medications"));
        this.status         = Objects.requireNonNull(status,      "status");
        this.issuedAt       = Objects.requireNonNull(issuedAt,    "issuedAt");
        this.fulfilledAt    = fulfilledAt;
        this.fulfilledBy    = fulfilledBy;
        this.cancelledAt    = cancelledAt;
        this.cancelledBy    = cancelledBy;
        this.cancelledReason = cancelledReason;
        this.expiredAt      = expiredAt;
        this.version        = version;
    }

    public static Prescription issue(PatientId patientId, String patientName,
                                     UserId doctorId, String doctorName,
                                     List<MedicationItem> medications) {
        return new Prescription(PrescriptionId.generate(), patientId, patientName,
                doctorId, doctorName, medications,
                PrescriptionStatus.ISSUED, Instant.now(),
                null, null, null, null, null, null, null);
    }

    @SuppressWarnings("java:S107")
    public static Prescription reconstitute(PrescriptionId id, PatientId patientId, String patientName,
                                            UserId doctorId, String doctorName, List<MedicationItem> medications,
                                            PrescriptionStatus status, Instant issuedAt,
                                            Instant fulfilledAt, UserId fulfilledBy,
                                            Instant cancelledAt, UserId cancelledBy, String cancelledReason,
                                            Instant expiredAt, Long version) {
        return new Prescription(id, patientId, patientName, doctorId, doctorName, medications,
                status, issuedAt, fulfilledAt, fulfilledBy, cancelledAt, cancelledBy, cancelledReason,
                expiredAt, version);
    }

    public Prescription fulfill(UserId actor) {
        if (!isFulfillable()) {
            throw new InvalidPrescriptionStateException("fulfill", status);
        }
        return new Prescription(id, patientId, patientName, doctorId, doctorName, medications,
                PrescriptionStatus.FULFILLED, issuedAt,
                Instant.now(), actor, null, null, null, null, version);
    }

    public Prescription cancel(UserId actor, String reason) {
        if (!isCancellable()) {
            throw new InvalidPrescriptionStateException("cancel", status);
        }
        return new Prescription(id, patientId, patientName, doctorId, doctorName, medications,
                PrescriptionStatus.CANCELLED, issuedAt,
                null, null, Instant.now(), actor, reason, null, version);
    }

    public Prescription expire() {
        if (!isExpirable()) {
            throw new InvalidPrescriptionStateException("expire", status);
        }
        return new Prescription(id, patientId, patientName, doctorId, doctorName, medications,
                PrescriptionStatus.EXPIRED, issuedAt,
                null, null, null, null, null, Instant.now(), version);
    }

    public boolean isFulfillable() {
        return status == PrescriptionStatus.ISSUED;
    }

    public boolean isCancellable() {
        return status == PrescriptionStatus.ISSUED;
    }

    public boolean isExpirable() {
        return status == PrescriptionStatus.ISSUED;
    }

    public PrescriptionId getId()          { return id; }
    public PatientId getPatientId()        { return patientId; }
    public String getPatientName()         { return patientName; }
    public UserId getDoctorId()            { return doctorId; }
    public String getDoctorName()          { return doctorName; }
    public List<MedicationItem> getMedications() { return medications; }
    public PrescriptionStatus getStatus()  { return status; }
    public Instant getIssuedAt()           { return issuedAt; }
    public Instant getFulfilledAt()        { return fulfilledAt; }
    public UserId getFulfilledBy()         { return fulfilledBy; }
    public Instant getCancelledAt()        { return cancelledAt; }
    public UserId getCancelledBy()         { return cancelledBy; }
    public String getCancelledReason()     { return cancelledReason; }
    public Instant getExpiredAt()          { return expiredAt; }
    public Long getVersion()               { return version; }
}
