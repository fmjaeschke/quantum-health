package net.fmjaeschke.quantumhealth.application.ports.in;

import net.fmjaeschke.quantumhealth.domain.model.Prescription;
import net.fmjaeschke.quantumhealth.domain.model.PrescriptionId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

public interface ReadPrescriptionUseCase {
    Prescription findById(PrescriptionId id, UserId actor);
}
