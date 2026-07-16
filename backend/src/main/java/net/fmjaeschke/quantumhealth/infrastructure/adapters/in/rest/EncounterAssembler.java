package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest;

import io.quarkus.hal.HalEntityWrapper;
import io.quarkus.hal.HalLink;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.core.UriInfo;
import net.fmjaeschke.quantumhealth.application.ports.out.AccessPolicy;
import net.fmjaeschke.quantumhealth.domain.model.Encounter;
import net.fmjaeschke.quantumhealth.domain.model.NoteVersion;
import net.fmjaeschke.quantumhealth.domain.model.Permission;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.EncounterResponse;

import java.util.LinkedHashMap;

@RequestScoped
public class EncounterAssembler {

    private final AccessPolicy accessPolicy;

    public EncounterAssembler(AccessPolicy accessPolicy) {
        this.accessPolicy = accessPolicy;
    }

    public HalEntityWrapper<EncounterResponse> toHal(Encounter encounter, UserId actor, UriInfo uriInfo) {
        var response = toResponse(encounter);
        var selfUri = uriInfo.getBaseUriBuilder()
                .path("encounters")
                .path(encounter.getId().value().toString())
                .build()
                .toString();

        var links = new LinkedHashMap<String, HalLink>();
        links.put("self", new HalLink(selfUri, null, null));

        if (encounter.getCompletedAt().isEmpty()
                && accessPolicy.isAllowed(Permission.WRITE_ENCOUNTER, actor)
                && accessPolicy.mayAccessOwnedBy(encounter.getDoctorId(), actor)) {
            links.put("add-clinical-note", new HalLink(selfUri + "/notes", null, null));
        }

        if (encounter.getCompletedAt().isEmpty()
                && accessPolicy.isAllowed(Permission.COMPLETE_ENCOUNTER, actor)
                && accessPolicy.mayAccessOwnedBy(encounter.getDoctorId(), actor)) {
            links.put("complete-encounter", new HalLink(selfUri + "/complete", null, null));
        }

        return new HalEntityWrapper<>(response, links);
    }

    private EncounterResponse toResponse(Encounter e) {
        return new EncounterResponse(
                e.getId().value(),
                e.getAppointmentId().value(),
                e.getDoctorId().value(),
                e.getPatientId().value(),
                e.getCompletedAt().orElse(null),
                e.getLatestNote().map(NoteVersion::content).orElse(null));
    }
}
