package net.fmjaeschke.quantumhealth.domain.exception;

import net.fmjaeschke.quantumhealth.domain.model.EncounterId;

public class EncounterCompletedException extends RuntimeException {

    private final EncounterId encounterId;

    public EncounterCompletedException(EncounterId encounterId) {
        super("Cannot add note to completed encounter: " + encounterId.value());
        this.encounterId = encounterId;
    }

    public EncounterId getEncounterId() { return encounterId; }
}
