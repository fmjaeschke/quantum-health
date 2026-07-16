package net.fmjaeschke.quantumhealth.application.ports.out;

import net.fmjaeschke.quantumhealth.domain.model.AppointmentId;
import net.fmjaeschke.quantumhealth.domain.model.Encounter;
import net.fmjaeschke.quantumhealth.domain.model.EncounterId;

import java.util.Optional;

public interface EncounterRepository {
    Encounter saveNew(Encounter encounter);
    Encounter save(Encounter encounter);
    Optional<Encounter> findById(EncounterId id);
    Optional<Encounter> findByAppointmentId(AppointmentId appointmentId);
}
