package net.fmjaeschke.quantumhealth.domain.model;

import java.time.Instant;

public final class Disposition {

    public final PrescriptionStatus status;
    public final Instant fulfilledAt;
    public final UserId fulfilledBy;
    public final Instant cancelledAt;
    public final UserId cancelledBy;
    public final String cancelledReason;
    public final Instant expiredAt;

    private Disposition(PrescriptionStatus status,
                        Instant fulfilledAt, UserId fulfilledBy,
                        Instant cancelledAt, UserId cancelledBy, String cancelledReason,
                        Instant expiredAt) {
        this.status = status;
        this.fulfilledAt = fulfilledAt;
        this.fulfilledBy = fulfilledBy;
        this.cancelledAt = cancelledAt;
        this.cancelledBy = cancelledBy;
        this.cancelledReason = cancelledReason;
        this.expiredAt = expiredAt;
    }

    public static Disposition issued() {
        return new Disposition(PrescriptionStatus.ISSUED, null, null, null, null, null, null);
    }

    public static Disposition fulfilled(UserId actor) {
        return new Disposition(PrescriptionStatus.FULFILLED, Instant.now(), actor, null, null, null, null);
    }

    public static Disposition cancelled(UserId actor, String reason) {
        return new Disposition(PrescriptionStatus.CANCELLED, null, null, Instant.now(), actor, reason, null);
    }

    public static Disposition expired() {
        return new Disposition(PrescriptionStatus.EXPIRED, null, null, null, null, null, Instant.now());
    }

    /** Used by the persistence layer to reconstitute from stored columns. */
    public static Disposition reconstituted(PrescriptionStatus status,
                                            Instant fulfilledAt, UserId fulfilledBy,
                                            Instant cancelledAt, UserId cancelledBy, String cancelledReason,
                                            Instant expiredAt) {
        return new Disposition(status, fulfilledAt, fulfilledBy, cancelledAt, cancelledBy, cancelledReason, expiredAt);
    }
}
