package net.fmjaeschke.quantumhealth.application.ports.out;

import net.fmjaeschke.quantumhealth.domain.model.EncounterId;
import net.fmjaeschke.quantumhealth.domain.model.NoteVersion;

import java.util.List;

public interface EncounterNoteRepository {
    NoteVersion save(EncounterId encounterId, NoteVersion note);
    List<NoteVersion> findByEncounterId(EncounterId encounterId);
}
