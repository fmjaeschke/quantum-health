package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

@RequestScoped
public class ActorProducer {

    private final SecurityIdentity identity;

    public ActorProducer(SecurityIdentity identity) {
        this.identity = identity;
    }

    @Produces
    UserId actor() {
        return UserId.of(identity.getPrincipal().getName());
    }
}
