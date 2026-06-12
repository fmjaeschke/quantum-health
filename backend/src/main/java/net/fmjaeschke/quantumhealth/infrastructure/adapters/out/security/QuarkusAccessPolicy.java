package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.security;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import net.fmjaeschke.quantumhealth.application.Permission;
import net.fmjaeschke.quantumhealth.application.exception.AccessDeniedException;
import net.fmjaeschke.quantumhealth.application.ports.out.AccessPolicy;
import net.fmjaeschke.quantumhealth.application.ports.out.AppointmentRepository;
import net.fmjaeschke.quantumhealth.application.ports.out.PrescriptionRepository;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.PrescriptionId;
import net.fmjaeschke.quantumhealth.domain.model.ResourceId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

import java.util.Objects;

@ApplicationScoped
public class QuarkusAccessPolicy implements AccessPolicy {

    private final SecurityIdentity identity;
    private final AppointmentRepository appointments;
    private final PrescriptionRepository prescriptions;

    public QuarkusAccessPolicy(SecurityIdentity identity, AppointmentRepository appointments,
                               PrescriptionRepository prescriptions) {
        this.identity = identity;
        this.appointments = appointments;
        this.prescriptions = prescriptions;
    }

    @Override
    public boolean isAllowed(Permission permission, UserId actor) {
        return switch (permission) {
            case CONFIRM_APPOINTMENT, CHECK_IN_PATIENT -> isClerk() || isAdmin();
            case START_ENCOUNTER      -> isDoctor();
            case CANCEL_APPOINTMENT   -> isClerk() || isDoctor() || isAdmin();
            case CANCEL_PRESCRIPTION  -> isDoctor() || isAdmin();
            case DISPENSE_MEDICATION  -> isPharmacist();
            default -> false;
        };
    }

    @Override
    public void check(Permission permission, UserId actor, ResourceId resource) {
        if (Objects.requireNonNull(permission) == Permission.READ_PATIENT) {
            checkReadPatient(actor, (PatientId) resource);
        } else if (permission == Permission.CANCEL_PRESCRIPTION) {
            checkCancelPrescription(actor, (PrescriptionId) resource);
        } else if (permission == Permission.DISPENSE_MEDICATION) {
            // role check via @RolesAllowed is the sole gate; any pharmacist may fulfill any prescription
        }
    }

    private void checkCancelPrescription(UserId actor, PrescriptionId prescriptionId) {
        if (isAdmin()) return;
        prescriptions.findById(prescriptionId).ifPresent(p -> {
            if (!p.getDoctorId().equals(actor)) {
                deny("cancel prescription " + prescriptionId.value());
            }
        });
    }

    private void checkReadPatient(UserId actor, PatientId patientId) {
        if (!isDoctor())
            return;  // only doctors need instance check
        // Ever-treated model: any appointment (including historical/cancelled) grants read access.
        // Doctors retain visibility once a care relationship has been established.
        if (!appointments.existsByDoctorAndPatient(actor, patientId)) {
            deny("read patient " + patientId.value());
        }
    }

    @Override
    public boolean isDoctor() {
        return identity.hasRole("DOCTOR");
    }

    private boolean isClerk() {
        return identity.hasRole("CLERK");
    }

    private boolean isPharmacist() {
        return identity.hasRole("PHARMACIST");
    }

    private boolean isAdmin() {
        return identity.hasRole("ADMIN");
    }

    private void deny(String action) {
        throw new AccessDeniedException(action);
    }
}
