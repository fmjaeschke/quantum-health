package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import net.fmjaeschke.quantumhealth.domain.model.Appointment;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentId;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentStatus;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "qh_appointment")
public class JpaAppointment {

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

    @Column(name = "scheduled_at", nullable = false)
    public LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public AppointmentStatus status;

    public static JpaAppointment from(Appointment a) {
        var entity = new JpaAppointment();
        entity.id = a.getId().value();
        entity.patientId = a.getPatientId().value();
        entity.patientName = a.getPatientName();
        entity.doctorId = a.getDoctorId().value();
        entity.doctorName = a.getDoctorName();
        entity.scheduledAt = a.getScheduledAt();
        entity.status = a.getStatus();
        return entity;
    }

    public Appointment toDomain() {
        return Appointment.reconstitute(
                AppointmentId.of(id),
                PatientId.of(patientId),
                patientName,
                UserId.of(doctorId),
                doctorName,
                scheduledAt,
                status);
    }
}
