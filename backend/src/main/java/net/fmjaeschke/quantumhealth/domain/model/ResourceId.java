package net.fmjaeschke.quantumhealth.domain.model;

public sealed interface ResourceId permits PatientId, AppointmentId, PrescriptionId, EncounterId {
}
