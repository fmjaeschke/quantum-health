package net.fmjaeschke.quantumhealth.domain.model;

public enum Permission {
    // Patient
    REGISTER_PATIENT,

    // Appointment
    SCHEDULE_APPOINTMENT,
    CONFIRM_APPOINTMENT,
    CANCEL_APPOINTMENT,
    CHECK_IN_PATIENT,

    // Encounter
    START_ENCOUNTER,
    WRITE_ENCOUNTER,
    READ_ENCOUNTER,

    // Prescription
    CANCEL_PRESCRIPTION,

    // Module-scoped (role check sufficient)
    DISPENSE_MEDICATION,
    PROCESS_BILLING,
}
