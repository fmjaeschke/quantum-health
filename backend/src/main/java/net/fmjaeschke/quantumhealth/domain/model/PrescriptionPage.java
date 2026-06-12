package net.fmjaeschke.quantumhealth.domain.model;

import java.util.List;

public record PrescriptionPage(List<Prescription> prescriptions, long totalElements, int page, int pageSize) {}
