package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest;

import io.quarkus.hal.HalEntityWrapper;
import io.quarkus.hal.HalLink;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.core.UriInfo;
import net.fmjaeschke.quantumhealth.domain.model.Permission;
import net.fmjaeschke.quantumhealth.application.ports.out.AccessPolicy;
import net.fmjaeschke.quantumhealth.application.ports.out.EncounterRepository;
import net.fmjaeschke.quantumhealth.domain.model.Appointment;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentPage;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.AppointmentListResponse;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.AppointmentListResponse.Embedded;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.AppointmentListResponse.Link;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.AppointmentListResponse.Links;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.AppointmentResponse;

import java.util.LinkedHashMap;

@RequestScoped
public class AppointmentAssembler {

    private final AccessPolicy accessPolicy;
    private final EncounterRepository encounterRepository;

    public AppointmentAssembler(AccessPolicy accessPolicy, EncounterRepository encounterRepository) {
        this.accessPolicy = accessPolicy;
        this.encounterRepository = encounterRepository;
    }

    public HalEntityWrapper<AppointmentResponse> toHal(Appointment appointment, UserId actor, UriInfo uriInfo) {
        var response = toResponse(appointment);
        var selfUri = uriInfo.getBaseUriBuilder()
                .path("appointments")
                .path(appointment.getId().value().toString())
                .build()
                .toString();

        var links = new LinkedHashMap<String, HalLink>();
        links.put("self", new HalLink(selfUri, null, null));
        if (appointment.isConfirmable() && accessPolicy.isAllowed(Permission.CONFIRM_APPOINTMENT, actor)) {
            links.put("confirm", new HalLink(selfUri + "/confirm", null, null));
        }
        if (appointment.isCheckInnable() && accessPolicy.isAllowed(Permission.CHECK_IN_PATIENT, actor)) {
            links.put("check-in", new HalLink(selfUri + "/check-in", null, null));
        }
        if (appointment.isStartable() && accessPolicy.isAllowed(Permission.START_ENCOUNTER, actor)
                && accessPolicy.mayAccessOwnedBy(appointment.getDoctorId(), actor)) {
            links.put("start", new HalLink(selfUri + "/start", null, null));
        }
        if (appointment.isCancellable() && accessPolicy.isAllowed(Permission.CANCEL_APPOINTMENT, actor)
                && accessPolicy.mayAccessOwnedBy(appointment.getDoctorId(), actor)) {
            links.put("cancel", new HalLink(selfUri + "/cancel", null, null));
        }
        encounterRepository.findByAppointmentId(appointment.getId())
                .filter(encounter -> encounter.getCompletedAt().isEmpty())
                .ifPresent(encounter -> {
                    var encounterUri = uriInfo.getBaseUriBuilder()
                            .path("encounters")
                            .path(encounter.getId().value().toString())
                            .build()
                            .toString();
                    links.put("medical-encounter", new HalLink(encounterUri, null, null));
                });

        return new HalEntityWrapper<>(response, links);
    }

    public AppointmentListResponse toListResponse(AppointmentPage page, UriInfo uriInfo) {
        var responses = page.appointments().stream().map(this::toResponse).toList();
        var selfUri = uriInfo.getAbsolutePath().toString();
        return new AppointmentListResponse(
                new Embedded(responses), new Links(new Link(selfUri)),
                page.page(), page.pageSize(), page.totalElements());
    }

    private AppointmentResponse toResponse(Appointment a) {
        return new AppointmentResponse(
                a.getId().value(),
                a.getPatientId().value(),
                a.getPatientName(),
                a.getDoctorId().value(),
                a.getDoctorName(),
                a.getScheduledAt(),
                a.getReason(),
                a.getStatus());
    }
}
