package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.security;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.fmjaeschke.quantumhealth.application.Permission;
import net.fmjaeschke.quantumhealth.application.exception.AccessDeniedException;
import net.fmjaeschke.quantumhealth.application.ports.out.AccessPolicy;
import net.fmjaeschke.quantumhealth.application.ports.out.AppointmentRepository;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.ResourceId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

@ApplicationScoped
public class QuarkusAccessPolicy implements AccessPolicy {

    @Inject
    SecurityIdentity identity;

    @Inject
    AppointmentRepository appointments;

    @Override
    public void check(Permission permission, UserId actor, ResourceId resource) {
        switch (permission) {
            case READ_PATIENT -> checkReadPatient(actor, (PatientId) resource);
            default -> {
            }  // role check at REST layer is enough
        }
    }

    private void checkReadPatient(UserId actor, PatientId patientId) {
        if (!identity.hasRole("DOCTOR"))
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
