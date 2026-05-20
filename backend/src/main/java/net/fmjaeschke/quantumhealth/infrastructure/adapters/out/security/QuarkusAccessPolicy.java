package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.security;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import net.fmjaeschke.quantumhealth.application.Permission;
import net.fmjaeschke.quantumhealth.application.exception.AccessDeniedException;
import net.fmjaeschke.quantumhealth.application.ports.out.AccessPolicy;
import net.fmjaeschke.quantumhealth.application.ports.out.AppointmentRepository;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.ResourceId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

@ApplicationScoped
public class QuarkusAccessPolicy implements AccessPolicy {

    private final SecurityIdentity identity;
    private final AppointmentRepository appointments;

    public QuarkusAccessPolicy(SecurityIdentity identity, AppointmentRepository appointments) {
        this.identity = identity;
        this.appointments = appointments;
    }

    @Override
    public boolean isAllowed(Permission permission, UserId actor) {
        return switch (permission) {
            case CONFIRM_APPOINTMENT  -> identity.hasRole("CLERK") || identity.hasRole("ADMIN");
            case CHECK_IN_PATIENT     -> identity.hasRole("CLERK") || identity.hasRole("ADMIN");
            case START_ENCOUNTER      -> isDoctor(actor);
            case CANCEL_APPOINTMENT   -> identity.hasRole("CLERK") || isDoctor(actor) || identity.hasRole("ADMIN");
            default -> false;
        };
    }

    @Override
    public void check(Permission permission, UserId actor, ResourceId resource) {
        switch (permission) {
            case READ_PATIENT -> checkReadPatient(actor, (PatientId) resource);
            default -> {
            }  // role check at REST layer is enough
        }
    }

    private void checkReadPatient(UserId actor, PatientId patientId) {
        if (!isDoctor(actor))
            return;  // only doctors need instance check
        if (!appointments.existsByDoctorAndPatient(actor, patientId)) {
            deny("read patient " + patientId.value());
        }
    }

    @Override
    public boolean isDoctor(UserId actor) {
        return identity.hasRole("DOCTOR");
    }

    private void deny(String action) {
        throw new AccessDeniedException(action);
    }
}
