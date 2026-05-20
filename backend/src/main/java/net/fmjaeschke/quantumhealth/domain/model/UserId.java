package net.fmjaeschke.quantumhealth.domain.model;

import java.util.Objects;

public class UserId {
    private String value;

    protected UserId() {
        // default constructor for CDI
    }

    private UserId(String value) {
        this.value = value;
    }

    public static UserId of(String value) {
        return new UserId(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UserId userId)) return false;
        return Objects.equals(value, userId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
