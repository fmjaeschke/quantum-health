package net.fmjaeschke.quantumhealth.application.ports.out;

/**
 * Outbound port for publishing domain events. Kept minimal and payload-agnostic so it can be
 * reused by future cross-context triggers (e.g. audit logging), not just billing.
 */
public interface DomainEventPublisher {
    <T> void publish(T event);
}
