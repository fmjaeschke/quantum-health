package net.fmjaeschke.quantumhealth.application.ports.in;

import net.fmjaeschke.quantumhealth.domain.model.Prescription;
import net.fmjaeschke.quantumhealth.domain.model.PrescriptionId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

public interface FulfillPrescriptionUseCase {
    Prescription fulfill(PrescriptionId id, UserId actor);
}
