package net.fmjaeschke.quantumhealth.application.ports.in;

import net.fmjaeschke.quantumhealth.domain.model.Patient;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

import java.time.LocalDate;

public interface RegisterPatientUseCase {
    Patient register(UserId actor, String firstName, String lastName, LocalDate dateOfBirth);
}
