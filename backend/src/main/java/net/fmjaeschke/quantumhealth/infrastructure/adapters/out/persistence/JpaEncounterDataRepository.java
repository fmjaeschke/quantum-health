package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Update;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaEncounterDataRepository extends BasicRepository<JpaEncounter, UUID> {

    @Insert
    JpaEncounter insert(JpaEncounter entity);

    @Update
    JpaEncounter update(JpaEncounter entity);

    @Find
    Optional<JpaEncounter> findByAppointmentId(UUID appointmentId);
}
