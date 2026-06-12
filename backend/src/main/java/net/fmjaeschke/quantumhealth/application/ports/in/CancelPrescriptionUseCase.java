package net.fmjaeschke.quantumhealth.application.ports.in;

import net.fmjaeschke.quantumhealth.domain.model.Prescription;
import net.fmjaeschke.quantumhealth.domain.model.PrescriptionId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

public interface CancelPrescriptionUseCase {
    Prescription cancel(PrescriptionId id, UserId actor, String reason);
}
