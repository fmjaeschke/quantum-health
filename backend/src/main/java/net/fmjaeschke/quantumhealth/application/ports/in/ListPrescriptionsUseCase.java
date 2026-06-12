package net.fmjaeschke.quantumhealth.application.ports.in;

import net.fmjaeschke.quantumhealth.domain.model.PrescriptionPage;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

public interface ListPrescriptionsUseCase {
    PrescriptionPage list(int page, int pageSize, UserId actor);
}
