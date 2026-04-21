package net.fmjaeschke.quantumhealth.domain.model;

import java.time.LocalDate;
import java.util.Optional;

public record PatientQuery(Optional<String> search, Optional<LocalDate> dateOfBirth, int page, int size,
                           SortField sortField, SortDirection sortDirection) {
    public enum SortField {
        FIRST_NAME("firstName"),
        LAST_NAME("lastName"),
        DATE_OF_BIRTH("dateOfBirth");

        public final String jpqlField;

        SortField(String jpqlField) {
            this.jpqlField = jpqlField;
        }
    }

    public enum SortDirection {
        ASC,
        DESC
    }
}
