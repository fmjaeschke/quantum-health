package net.fmjaeschke.quantumhealth.domain.model;

import net.fmjaeschke.quantumhealth.domain.exception.EncounterCompletedException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class Encounter {

    private final EncounterId id;
    private final AppointmentId appointmentId;
    private final UserId doctorId;
    private final PatientId patientId;
    private final Instant completedAt;
    private final List<NoteVersion> notes;

    private Encounter(EncounterId id, AppointmentId appointmentId, UserId doctorId,
                      PatientId patientId, Instant completedAt, List<NoteVersion> notes) {
        this.id            = Objects.requireNonNull(id,            "id");
        this.appointmentId = Objects.requireNonNull(appointmentId, "appointmentId");
        this.doctorId      = Objects.requireNonNull(doctorId,      "doctorId");
        this.patientId     = Objects.requireNonNull(patientId,     "patientId");
        this.completedAt   = completedAt;
        this.notes         = List.copyOf(Objects.requireNonNull(notes, "notes"));
    }

    public static Encounter create(AppointmentId appointmentId, UserId doctorId, PatientId patientId) {
        return new Encounter(EncounterId.generate(), appointmentId, doctorId, patientId, null, List.of());
    }

    public static Encounter reconstitute(EncounterId id, AppointmentId appointmentId, UserId doctorId,
                                         PatientId patientId, Instant completedAt, List<NoteVersion> notes) {
        return new Encounter(id, appointmentId, doctorId, patientId, completedAt, notes);
    }

    /**
     * Hydration composition only: attaches a freshly-loaded notes list to this encounter.
     * Not a business transition — does not run the {@link #addNote} completion check.
     */
    public Encounter withNotes(List<NoteVersion> notes) {
        return new Encounter(id, appointmentId, doctorId, patientId, completedAt, notes);
    }

    public Encounter addNote(String content, UserId authorId, Instant at) {
        if (completedAt != null) {
            throw new EncounterCompletedException(id);
        }
        int nextVersion = notes.isEmpty() ? 1 : notes.get(notes.size() - 1).version() + 1;
        var updatedNotes = new ArrayList<>(notes);
        updatedNotes.add(new NoteVersion(nextVersion, content, authorId, at));
        return new Encounter(id, appointmentId, doctorId, patientId, completedAt, updatedNotes);
    }

    public Encounter complete(Instant at) {
        if (completedAt != null) {
            throw new EncounterCompletedException(id);
        }
        return new Encounter(id, appointmentId, doctorId, patientId, at, notes);
    }

    public EncounterId getId() { return id; }
    public AppointmentId getAppointmentId() { return appointmentId; }
    public UserId getDoctorId() { return doctorId; }
    public PatientId getPatientId() { return patientId; }
    public Optional<Instant> getCompletedAt() { return Optional.ofNullable(completedAt); }
    public List<NoteVersion> getNotes() { return notes; }
    public Optional<NoteVersion> getLatestNote() {
        return notes.isEmpty() ? Optional.empty() : Optional.of(notes.get(notes.size() - 1));
    }
}
