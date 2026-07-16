package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.security;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import net.fmjaeschke.quantumhealth.application.exception.AccessDeniedException;
import net.fmjaeschke.quantumhealth.application.ports.out.AccessPolicy;
import net.fmjaeschke.quantumhealth.application.ports.out.AppointmentRepository;
import net.fmjaeschke.quantumhealth.application.ports.out.EncounterRepository;
import net.fmjaeschke.quantumhealth.application.ports.out.PrescriptionRepository;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentId;
import net.fmjaeschke.quantumhealth.domain.model.EncounterId;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.Permission;
import net.fmjaeschke.quantumhealth.domain.model.PrescriptionId;
import net.fmjaeschke.quantumhealth.domain.model.ResourceId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

import java.util.function.Consumer;

@ApplicationScoped
public class QuarkusAccessPolicy implements AccessPolicy {

    private final SecurityIdentity identity;
    private final AppointmentRepository appointments;
    private final PrescriptionRepository prescriptions;
    private final EncounterRepository encounters;

    public QuarkusAccessPolicy(SecurityIdentity identity, AppointmentRepository appointments,
                               PrescriptionRepository prescriptions, EncounterRepository encounters) {
        this.identity = identity;
        this.appointments = appointments;
        this.prescriptions = prescriptions;
        this.encounters = encounters;
    }

    @Override
    public boolean isAllowed(Permission permission, UserId actor) {
        return switch (permission) {
            case CONFIRM_APPOINTMENT, CHECK_IN_PATIENT -> isClerk() || isAdmin();
            case START_ENCOUNTER      -> isDoctor();
            case WRITE_ENCOUNTER      -> isDoctor();
            case COMPLETE_ENCOUNTER   -> isDoctor();
            case CANCEL_APPOINTMENT   -> isClerk() || isDoctor() || isAdmin();
            case CANCEL_PRESCRIPTION  -> isDoctor() || isAdmin();
            case DISPENSE_MEDICATION  -> isPharmacist();
            default -> false;
        };
    }

    @Override
    public void check(Permission permission, UserId actor, ResourceId resource) {
        // Assigning the switch to an (unnamed) variable forces the compiler to treat it as a switch
        // expression, which rejects the method at compile time if a Permission is left unhandled.
        Void _ = switch (permission) {
            case CANCEL_PRESCRIPTION -> { checkTyped(resource, PrescriptionId.class, id -> checkCancelPrescription(actor, id)); yield null; }
            case CANCEL_APPOINTMENT -> { checkTyped(resource, AppointmentId.class,  id -> checkDoctorOwnsAppointment(actor, id, "cancel appointment")); yield null; }
            case START_ENCOUNTER    -> { checkTyped(resource, AppointmentId.class,  id -> checkStartEncounter(actor, id));   yield null; }
            case WRITE_ENCOUNTER    -> { checkTyped(resource, EncounterId.class,    id -> checkWriteEncounter(actor, id));   yield null; }
            case COMPLETE_ENCOUNTER -> { checkTyped(resource, EncounterId.class,    id -> checkCompleteEncounter(actor, id)); yield null; }
            // role check via @RolesAllowed is the sole gate; any pharmacist may fulfill any prescription
            case DISPENSE_MEDICATION -> null;
            // role check via @RolesAllowed is the sole gate; isAllowed() only ever permits
            // CLERK/ADMIN for these two, so there is no DOCTOR branch to scope
            case CONFIRM_APPOINTMENT, CHECK_IN_PATIENT -> null;
            case REGISTER_PATIENT, SCHEDULE_APPOINTMENT,
                 READ_ENCOUNTER, PROCESS_BILLING ->
                    throw new IllegalStateException("check() is not yet wired for permission " + permission);
        };
    }

    /**
     * Narrows {@code resource} to the resource type the permission expects, then runs its check.
     * Fails closed by construction: a resource of the wrong type is denied rather than silently
     * skipped, so a mismatched permission/resource pairing can never no-op.
     */
    private <T extends ResourceId> void checkTyped(ResourceId resource, Class<T> type, Consumer<T> check) {
        if (type.isInstance(resource)) {
            check.accept(type.cast(resource));
        } else {
            deny("expected " + type.getSimpleName() + " resource, got " + resource);
        }
    }

    private void checkCancelPrescription(UserId actor, PrescriptionId prescriptionId) {
        if (isAdmin()) return;
        prescriptions.findById(prescriptionId)
                .ifPresent(p -> denyIfNotOwner(p.getDoctorId(), actor, "cancel prescription " + prescriptionId.value()));
    }

    // Doctor-ownership check for appointment cancellation: non-doctors (clerk/admin)
    // get blanket access, a doctor only their own appointments. `action` is the deny-message prefix.
    // (Nurses can't reach this method at all: cancel's @RolesAllowed excludes NURSE.)
    //
    // Note: the appointment is re-fetched here even though the calling use case fetches it again
    // right after check() returns. Both reads are by primary key within the same @Transactional
    // persistence context, so the second is an L1-cache hit (no extra SQL). This is only cheap as
    // long as that holds — moving the check to a different transaction scope or to a non-PK query
    // would turn it into a genuine doubled query. Applies to checkStartEncounter below too.
    private void checkDoctorOwnsAppointment(UserId actor, AppointmentId appointmentId, String action) {
        if (!isDoctor()) return;  // non-doctor roles get blanket access
        appointments.findById(appointmentId)
                .ifPresent(a -> denyIfNotOwner(a.getDoctorId(), actor, action + " " + appointmentId.value()));
    }

    private void checkStartEncounter(UserId actor, AppointmentId appointmentId) {
        // isAllowed() only ever permits DOCTOR for this permission, so the actor must
        // always be the appointment's assigned doctor; there is no blanket-access role.
        appointments.findById(appointmentId)
                .ifPresent(a -> denyIfNotOwner(a.getDoctorId(), actor, "start encounter " + appointmentId.value()));
    }

    private void checkWriteEncounter(UserId actor, EncounterId encounterId) {
        // isAllowed() only ever permits DOCTOR for this permission, so the actor must
        // always be the encounter's assigned doctor; there is no blanket-access role.
        encounters.findById(encounterId)
                .ifPresent(e -> denyIfNotOwner(e.getDoctorId(), actor, "write encounter note " + encounterId.value()));
    }

    private void checkCompleteEncounter(UserId actor, EncounterId encounterId) {
        // isAllowed() only ever permits DOCTOR for this permission, so the actor must
        // always be the encounter's assigned doctor; there is no blanket-access role.
        encounters.findById(encounterId)
                .ifPresent(e -> denyIfNotOwner(e.getDoctorId(), actor, "complete encounter " + encounterId.value()));
    }

    @Override
    public boolean isDoctor() {
        return identity.hasRole("DOCTOR");
    }

    @Override
    public boolean mayAccessOwnedBy(UserId resourceOwner, UserId actor) {
        return !isDoctor() || resourceOwner.equals(actor);
    }

    @Override
    public boolean mayAccessPatient(UserId actor, PatientId patientId) {
        // Ever-treated model: any appointment (including historical/cancelled) grants read access.
        // Doctors retain visibility once a care relationship has been established.
        return !isDoctor() || appointments.existsByDoctorAndPatient(actor, patientId);
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

    /**
     * Central instance-ownership rule for resource-scoped checks: a resource may only be acted on
     * by its assigned doctor. Kept in one place so a future change to the ownership rule (e.g. a
     * covering/on-call doctor) is applied once rather than across every checkXxx method. Role-level
     * exemptions (admin bypass, blanket clerk access) stay in the individual callers, since they
     * differ per action.
     */
    private void denyIfNotOwner(UserId resourceOwner, UserId actor, String action) {
        if (!resourceOwner.equals(actor)) {
            deny(action);
        }
    }
}
