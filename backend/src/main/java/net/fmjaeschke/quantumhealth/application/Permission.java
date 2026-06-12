package net.fmjaeschke.quantumhealth.application;

public enum Permission {
    // Patient
    REGISTER_PATIENT,
    READ_PATIENT,

    // Appointment
    SCHEDULE_APPOINTMENT,
    READ_APPOINTMENT,
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
