package net.fmjaeschke.quantumhealth.application.exception;

import net.fmjaeschke.quantumhealth.domain.model.EncounterId;

public class EncounterNotFoundException extends RuntimeException {
    public EncounterNotFoundException(EncounterId id) {
        super("Encounter not found: " + id.value());
    }
}
