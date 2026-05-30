package net.fmjaeschke.quantumhealth.application.ports.out;

import net.fmjaeschke.quantumhealth.application.Permission;
import net.fmjaeschke.quantumhealth.domain.model.ResourceId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

public interface AccessPolicy {
    /**
     * Throws AccessDeniedException if the actor is not permitted. No-op if permitted.
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
}
