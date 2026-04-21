package net.fmjaeschke.quantumhealth.domain.model;

import java.util.List;

public record PatientPage(List<Patient> patients, long totalElements, int page, int size) {
}
