package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest;

import io.quarkus.hal.HalEntityWrapper;
import io.quarkus.hal.HalLink;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.core.UriInfo;
import net.fmjaeschke.quantumhealth.domain.model.Appointment;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.AppointmentListResponse;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.AppointmentListResponse.Embedded;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.AppointmentListResponse.Link;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.AppointmentListResponse.Links;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.AppointmentResponse;

import java.util.LinkedHashMap;
import java.util.List;

@RequestScoped
public class AppointmentAssembler {

    public HalEntityWrapper<AppointmentResponse> toHal(Appointment appointment, UriInfo uriInfo) {
        var response = toResponse(appointment);
        var selfUri = uriInfo.getBaseUriBuilder()
                .path("appointments")
                .path(appointment.getId().value().toString())
                .build()
                .toString();

        var links = new LinkedHashMap<String, HalLink>();
        links.put("self", new HalLink(selfUri, null, null));
        if (appointment.isConfirmable()) {
            links.put("confirm", new HalLink(selfUri + "/confirm", null, null));
        }
        if (appointment.isCancellable()) {
            links.put("cancel", new HalLink(selfUri + "/cancel", null, null));
        }

        return new HalEntityWrapper<>(response, links);
    }

    public AppointmentListResponse toListResponse(List<Appointment> appointments, UriInfo uriInfo) {
        var responses = appointments.stream().map(this::toResponse).toList();
        var selfUri = uriInfo.getAbsolutePath().toString();
        return new AppointmentListResponse(new Embedded(responses), new Links(new Link(selfUri)));
    }

    private AppointmentResponse toResponse(Appointment a) {
        return new AppointmentResponse(
                a.getId().value(),
                a.getPatientId().value(),
                a.getPatientName(),
                a.getDoctorId().value(),
                a.getDoctorName(),
                a.getScheduledAt(),
                a.getStatus());
    }
}
