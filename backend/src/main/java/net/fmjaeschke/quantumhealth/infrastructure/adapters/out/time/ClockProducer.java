package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.time;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import java.time.Clock;

@ApplicationScoped
public class ClockProducer {

    @Produces
    @ApplicationScoped
    Clock clock() {
        return Clock.systemUTC();
    }
}
