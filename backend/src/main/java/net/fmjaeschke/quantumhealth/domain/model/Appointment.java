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
    private final AppointmentStatus status;

    private Appointment(AppointmentId id, PatientId patientId, String patientName,
                        UserId doctorId, String doctorName, LocalDateTime scheduledAt,
                        AppointmentStatus status) {
        this.id          = Objects.requireNonNull(id,          "id");
        this.patientId   = Objects.requireNonNull(patientId,   "patientId");
        this.patientName = Objects.requireNonNull(patientName, "patientName");
        this.doctorId    = Objects.requireNonNull(doctorId,    "doctorId");
        this.doctorName  = Objects.requireNonNull(doctorName,  "doctorName");
        this.scheduledAt = Objects.requireNonNull(scheduledAt, "scheduledAt");
        this.status      = Objects.requireNonNull(status,      "status");
    }

    public static Appointment schedule(PatientId patientId, String patientName,
                                       UserId doctorId, String doctorName,
                                       LocalDateTime scheduledAt) {
        return new Appointment(AppointmentId.generate(), patientId, patientName,
                doctorId, doctorName, scheduledAt, AppointmentStatus.SCHEDULED);
    }

    public static Appointment reconstitute(AppointmentId id, PatientId patientId,
                                           String patientName, UserId doctorId,
                                           String doctorName, LocalDateTime scheduledAt,
                                           AppointmentStatus status) {
        return new Appointment(id, patientId, patientName, doctorId, doctorName, scheduledAt, status);
    }

    public Appointment confirm() {
        if (!isConfirmable()) {
            throw new IllegalStateException("Can only confirm SCHEDULED appointments, current status: " + status);
        }
        return new Appointment(id, patientId, patientName, doctorId, doctorName, scheduledAt, AppointmentStatus.CONFIRMED);
    }

    public Appointment cancel() {
        if (!isCancellable()) {
            throw new IllegalStateException("Cannot cancel appointment in status: " + status);
        }
        return new Appointment(id, patientId, patientName, doctorId, doctorName, scheduledAt, AppointmentStatus.CANCELLED);
    }

    public boolean isConfirmable() {
        return status == AppointmentStatus.SCHEDULED;
    }

    public boolean isCancellable() {
        return status == AppointmentStatus.SCHEDULED || status == AppointmentStatus.CONFIRMED;
    }

    public AppointmentId getId() { return id; }
    public PatientId getPatientId() { return patientId; }
    public String getPatientName() { return patientName; }
    public UserId getDoctorId() { return doctorId; }
    public String getDoctorName() { return doctorName; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public AppointmentStatus getStatus() { return status; }
}
