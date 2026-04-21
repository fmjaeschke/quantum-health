package net.fmjaeschke.quantumhealth.application.exception;

public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String action) {
        super("Access denied: " + action);
    }
}
