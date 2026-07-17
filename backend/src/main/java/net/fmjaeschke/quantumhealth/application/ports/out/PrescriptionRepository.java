package net.fmjaeschke.quantumhealth.application.ports.out;

import net.fmjaeschke.quantumhealth.application.exception.ConcurrentModificationException;
import net.fmjaeschke.quantumhealth.domain.model.Prescription;
import net.fmjaeschke.quantumhealth.domain.model.PrescriptionId;
import net.fmjaeschke.quantumhealth.domain.model.PrescriptionPage;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PrescriptionRepository {
    Prescription saveNew(Prescription prescription);

    /**
     * @throws ConcurrentModificationException if {@code prescription} was modified since it was read,
     *                                          detected via optimistic locking
     */
    Prescription save(Prescription prescription);
    Optional<Prescription> findById(PrescriptionId id);
    PrescriptionPage findAll(int page, int pageSize, Optional<UserId> doctorId);
    List<Prescription> findStale(Instant threshold);

    /**
     * @throws ConcurrentModificationException if {@code prescription} was modified since it was read,
     *                                          detected via optimistic locking
     */
    void expireOne(Prescription prescription, Instant expiredAt);
}
