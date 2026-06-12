package net.fmjaeschke.quantumhealth.domain.model;

import java.time.LocalDate;

public final class Patient {

    private final PatientId id;
    private final String firstName;
    private final String lastName;
    private final LocalDate dateOfBirth;

    private Patient(PatientId id, String firstName, String lastName, LocalDate dateOfBirth) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
    }

    public static Patient register(String firstName, String lastName, LocalDate dateOfBirth) {
        return new Patient(PatientId.generate(), firstName, lastName, dateOfBirth);
    }

    /**
     * Reconstitutes a Patient from persistent storage — never generates a new id.
     */
    public static Patient reconstitute(PatientId id, String firstName, String lastName, LocalDate dateOfBirth) {
        return new Patient(id, firstName, lastName, dateOfBirth);
    }

    public PatientId getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }
}
