package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.fmjaeschke.quantumhealth.application.ports.out.EncounterNoteRepository;
import net.fmjaeschke.quantumhealth.domain.model.EncounterId;
import net.fmjaeschke.quantumhealth.domain.model.NoteVersion;

import java.util.List;

@ApplicationScoped
public class JpaEncounterNoteRepository implements EncounterNoteRepository {

    @Inject
    JpaEncounterNoteDataRepository dataRepository;

    @Override
    public NoteVersion save(EncounterId encounterId, NoteVersion note) {
        var entity = JpaEncounterNote.from(encounterId, note);
        return dataRepository.insert(entity).toDomain();
    }

    @Override
    public List<NoteVersion> findByEncounterId(EncounterId encounterId) {
        return dataRepository.findByEncounterId(encounterId.value()).stream()
                .map(JpaEncounterNote::toDomain).toList();
    }
}
