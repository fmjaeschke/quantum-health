package net.fmjaeschke.quantumhealth.application.ports.in;

import net.fmjaeschke.quantumhealth.domain.model.MedicationItem;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.Prescription;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

import java.util.List;

public interface IssuePrescriptionUseCase {
    Prescription issue(UserId actor, PatientId patientId, List<MedicationItem> medications);
}
