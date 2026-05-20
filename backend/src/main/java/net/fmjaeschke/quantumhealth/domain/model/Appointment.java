package net.fmjaeschke.quantumhealth.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

public final class Appointment {

    private final AppointmentId id;
    private final PatientId patientId;
    private final String patientName;
    private final UserId doctorId;
    private final String doctorName;
    private final LocalDateTime scheduledAt;
    private final String reason;
    private final AppointmentStatus status;

    private Appointment(AppointmentId id, PatientId patientId, String patientName,
                        UserId doctorId, String doctorName, LocalDateTime scheduledAt,
                        String reason, AppointmentStatus status) {
        this.id          = Objects.requireNonNull(id,          "id");
        this.patientId   = Objects.requireNonNull(patientId,   "patientId");
        this.patientName = Objects.requireNonNull(patientName, "patientName");
        this.doctorId    = Objects.requireNonNull(doctorId,    "doctorId");
        this.doctorName  = Objects.requireNonNull(doctorName,  "doctorName");
        this.scheduledAt = Objects.requireNonNull(scheduledAt, "scheduledAt");
        this.reason      = Objects.requireNonNull(reason,      "reason");
        this.status      = Objects.requireNonNull(status,      "status");
    }

    public static Appointment schedule(PatientId patientId, String patientName,
                                       UserId doctorId, String doctorName,
                                       LocalDateTime scheduledAt, String reason) {
        return new Appointment(AppointmentId.generate(), patientId, patientName,
                doctorId, doctorName, scheduledAt, reason, AppointmentStatus.PENDING);
    }

    public static Appointment reconstitute(AppointmentId id, PatientId patientId,
                                           String patientName, UserId doctorId,
                                           String doctorName, LocalDateTime scheduledAt,
                                           String reason, AppointmentStatus status) {
        return new Appointment(id, patientId, patientName, doctorId, doctorName, scheduledAt, reason, status);
    }

    public Appointment confirm() {
        if (!isConfirmable()) {
            throw new IllegalStateException("Can only confirm PENDING appointments, current status: " + status);
        }
        return new Appointment(id, patientId, patientName, doctorId, doctorName, scheduledAt, reason, AppointmentStatus.CONFIRMED);
    }

    public Appointment cancel() {
        if (!isCancellable()) {
            throw new IllegalStateException("Cannot cancel appointment in status: " + status);
        }
        return new Appointment(id, patientId, patientName, doctorId, doctorName, scheduledAt, reason, AppointmentStatus.CANCELLED);
    }

    public Appointment checkIn() {
        if (!isCheckInnable()) {
            throw new IllegalStateException("Can only check in CONFIRMED appointments, current status: " + status);
        }
        return new Appointment(id, patientId, patientName, doctorId, doctorName, scheduledAt, reason, AppointmentStatus.ARRIVED);
    }

    public Appointment start() {
        if (!isStartable()) {
            throw new IllegalStateException("Can only start CONFIRMED or ARRIVED appointments, current status: " + status);
        }
        return new Appointment(id, patientId, patientName, doctorId, doctorName, scheduledAt, reason, AppointmentStatus.IN_PROGRESS);
    }

    public boolean isConfirmable() {
        return status == AppointmentStatus.PENDING;
    }

    public boolean isCancellable() {
        return status == AppointmentStatus.PENDING || status == AppointmentStatus.CONFIRMED;
    }

    public boolean isCheckInnable() {
        return status == AppointmentStatus.CONFIRMED;
    }

    public boolean isStartable() {
        return status == AppointmentStatus.CONFIRMED || status == AppointmentStatus.ARRIVED;
    }

    public AppointmentId getId() { return id; }
    public PatientId getPatientId() { return patientId; }
    public String getPatientName() { return patientName; }
    public UserId getDoctorId() { return doctorId; }
    public String getDoctorName() { return doctorName; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public String getReason() { return reason; }
    public AppointmentStatus getStatus() { return status; }
}
