package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.scheduler;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import net.fmjaeschke.quantumhealth.application.ports.in.ExpirePrescriptionsUseCase;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Clock;
import java.time.Duration;

@ApplicationScoped
public class PrescriptionExpiryJob {

    private final ExpirePrescriptionsUseCase expireUseCase;
    private final Clock clock;

    @ConfigProperty(name = "quantum-health.prescription.expiry-after", defaultValue = "P30D")
    Duration expiryAfter;

    public PrescriptionExpiryJob(ExpirePrescriptionsUseCase expireUseCase, Clock clock) {
        this.expireUseCase = expireUseCase;
        this.clock = clock;
    }

    @Scheduled(identity = "prescription-expiry", cron = "0 0 0 * * ?")
    @Transactional
    public void expireStale() {
        expireUseCase.expireOlderThan(clock.instant().minus(expiryAfter));
    }
}
