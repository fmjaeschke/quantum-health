package net.fmjaeschke.quantumhealth.domain.model;

import java.util.List;

public record AppointmentPage(List<Appointment> appointments, long totalElements, int page, int pageSize) {
}
