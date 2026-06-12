package net.fmjaeschke.quantumhealth.application.ports.in;

import java.time.Instant;

public interface ExpirePrescriptionsUseCase {
    int expireOlderThan(Instant threshold);
}
