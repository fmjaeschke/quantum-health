package net.fmjaeschke.quantumhealth.application.ports.out;

import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.Permission;
import net.fmjaeschke.quantumhealth.domain.model.ResourceId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

public interface AccessPolicy {
    /**
     * Enforces resource-level access for {@code permission}.
     *
     * @throws net.fmjaeschke.quantumhealth.application.exception.AccessDeniedException
     *         if the actor is not permitted; no-op if permitted.
     * @throws IllegalStateException if resource-level enforcement for {@code permission} is not
     *         yet wired. Callers must confirm the permission is supported before relying on this
     *         method (see the switch in the implementation for the wired set).
     */
    void check(Permission permission, UserId actor, ResourceId resource);

    /**
     * Returns true if the actor is allowed to perform permission (role check only, no resource-level enforcement).
     */
    boolean isAllowed(Permission permission, UserId actor);

    /**
     * Returns true if the actor holds the DOCTOR role.
     */
    boolean isDoctor();

    /**
     * Instance-ownership rule for doctor-scoped resources: a doctor may only act on resources they
     * own, while non-doctor roles (clerk/admin) get blanket access. Central home for the
     * {@code !isDoctor() || resourceOwner.equals(actor)} predicate so callers (assemblers, use
     * cases) don't hand-roll it and drift apart.
     */
    boolean mayAccessOwnedBy(UserId resourceOwner, UserId actor);

    /**
     * Instance-access rule for patients: a doctor may only read a patient they have ever treated
     * (established via an existing appointment), while non-doctor roles get blanket access.
     * Query-based (unlike {@link #mayAccessOwnedBy}), so it's a dedicated method rather than folded
     * into that owner-equality predicate.
     */
    boolean mayAccessPatient(UserId actor, PatientId patientId);
}
