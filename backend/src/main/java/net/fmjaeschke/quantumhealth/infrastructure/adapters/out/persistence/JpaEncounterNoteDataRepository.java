package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaEncounterNoteDataRepository extends BasicRepository<JpaEncounterNote, UUID> {

    @Insert
    JpaEncounterNote insert(JpaEncounterNote entity);

    @Find
    @OrderBy("version")
    List<JpaEncounterNote> findByEncounterId(UUID encounterId);
}
