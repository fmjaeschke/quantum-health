package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentId;
import net.fmjaeschke.quantumhealth.domain.model.Encounter;
import net.fmjaeschke.quantumhealth.domain.model.EncounterId;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "qh_encounter")
public class JpaEncounter {

    @Id
    public UUID id;

    @Column(name = "appointment_id", nullable = false)
    public UUID appointmentId;

    @Column(name = "doctor_id", nullable = false)
    public String doctorId;

    @Column(name = "patient_id", nullable = false)
    public UUID patientId;

    @Column(name = "completed_at")
    public Instant completedAt;

    public static JpaEncounter from(Encounter e) {
        var entity = new JpaEncounter();
        entity.id            = e.getId().value();
        entity.appointmentId = e.getAppointmentId().value();
        entity.doctorId      = e.getDoctorId().value();
        entity.patientId     = e.getPatientId().value();
        entity.completedAt   = e.getCompletedAt().orElse(null);
        return entity;
    }

    public Encounter toDomain() {
        return Encounter.reconstitute(
                EncounterId.of(id),
                AppointmentId.of(appointmentId),
                UserId.of(doctorId),
                PatientId.of(patientId),
                completedAt,
                List.of());
    }
}
