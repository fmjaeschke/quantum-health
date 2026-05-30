package net.fmjaeschke.quantumhealth.application.exception;

import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

public class DuplicateAppointmentException extends RuntimeException {
    public DuplicateAppointmentException(UserId doctorId, PatientId patientId) {
        super("Active appointment already exists for doctor " + doctorId.value()
                + " and patient " + patientId.value());
    }
}
