package net.fmjaeschke.quantumhealth.application.ports.in;

import net.fmjaeschke.quantumhealth.domain.model.PatientPage;
import net.fmjaeschke.quantumhealth.domain.model.PatientQuery;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

public interface ListPatientUseCase {
    PatientPage listPatients(UserId actor, PatientQuery query);
}
