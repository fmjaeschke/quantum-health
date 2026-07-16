package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.event;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import net.fmjaeschke.quantumhealth.domain.model.EncounterCompletedEvent;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class RecordingEncounterCompletedObserver {
    private final List<EncounterCompletedEvent> received = new ArrayList<>();

    void onEncounterCompleted(@Observes EncounterCompletedEvent event) {
        received.add(event);
    }

    public List<EncounterCompletedEvent> getReceived() {
        return received;
    }
}
