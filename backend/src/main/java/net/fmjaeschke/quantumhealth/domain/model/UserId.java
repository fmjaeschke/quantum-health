package net.fmjaeschke.quantumhealth.domain.model;

public record UserId(String value) {
    public static UserId of(String value) {
        return new UserId(value);
    }
}
