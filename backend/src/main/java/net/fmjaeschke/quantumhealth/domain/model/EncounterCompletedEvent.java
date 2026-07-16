package net.fmjaeschke.quantumhealth.domain.model;

public record EncounterCompletedEvent(EncounterId encounterId, AppointmentId appointmentId,
                                      PatientId patientId, UserId doctorId) {}
