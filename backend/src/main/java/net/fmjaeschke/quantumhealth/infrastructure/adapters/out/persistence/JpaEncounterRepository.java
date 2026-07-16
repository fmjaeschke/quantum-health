package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.fmjaeschke.quantumhealth.application.ports.out.EncounterRepository;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentId;
import net.fmjaeschke.quantumhealth.domain.model.Encounter;
import net.fmjaeschke.quantumhealth.domain.model.EncounterId;

import java.util.Optional;

@ApplicationScoped
public class JpaEncounterRepository implements EncounterRepository {

    @Inject
    JpaEncounterDataRepository dataRepository;

    @Override
    public Encounter saveNew(Encounter encounter) {
        return dataRepository.insert(JpaEncounter.from(encounter)).toDomain();
    }

    @Override
    public Encounter save(Encounter encounter) {
        return dataRepository.update(JpaEncounter.from(encounter)).toDomain();
    }

    @Override
    public Optional<Encounter> findById(EncounterId id) {
        return dataRepository.findById(id.value()).map(JpaEncounter::toDomain);
    }

    @Override
    public Optional<Encounter> findByAppointmentId(AppointmentId appointmentId) {
        return dataRepository.findByAppointmentId(appointmentId.value()).map(JpaEncounter::toDomain);
    }
}
