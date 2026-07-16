package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import net.fmjaeschke.quantumhealth.domain.model.EncounterId;
import net.fmjaeschke.quantumhealth.domain.model.NoteVersion;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "qh_encounter_note")
public class JpaEncounterNote {

    @Id
    public UUID id;

    @Column(name = "encounter_id", nullable = false)
    public UUID encounterId;

    @Column(nullable = false)
    public int version;

    @Column(nullable = false, columnDefinition = "text")
    public String content;

    @Column(name = "author_id", nullable = false)
    public String authorId;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    public static JpaEncounterNote from(EncounterId encounterId, NoteVersion note) {
        var entity = new JpaEncounterNote();
        entity.id = UUID.randomUUID();
        entity.encounterId = encounterId.value();
        entity.version = note.version();
        entity.content = note.content();
        entity.authorId = note.authorId().value();
        entity.createdAt = note.createdAt();
        return entity;
    }

    public NoteVersion toDomain() {
        return new NoteVersion(version, content, UserId.of(authorId), createdAt);
    }
}
