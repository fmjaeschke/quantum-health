package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.event;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import net.fmjaeschke.quantumhealth.application.ports.out.DomainEventPublisher;

@ApplicationScoped
public class CdiDomainEventPublisher implements DomainEventPublisher {

    private final Event<Object> event;

    public CdiDomainEventPublisher(Event<Object> event) {
        this.event = event;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void publish(T event) {
        // Quarkus ArC resolves @Observes methods against the type passed to select(), not the
        // runtime class of the fired object, so firing through the raw Event<Object> injection
        // point alone would never reach a listener typed to the concrete event class.
        this.event.select((Class<Object>) event.getClass()).fire(event);
    }
}
