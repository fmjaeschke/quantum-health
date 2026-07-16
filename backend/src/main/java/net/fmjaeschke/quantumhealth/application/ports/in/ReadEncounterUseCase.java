package net.fmjaeschke.quantumhealth.application.ports.in;

import net.fmjaeschke.quantumhealth.domain.model.Encounter;
import net.fmjaeschke.quantumhealth.domain.model.EncounterId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

public interface ReadEncounterUseCase {
    Encounter findById(EncounterId id, UserId actor);
}
