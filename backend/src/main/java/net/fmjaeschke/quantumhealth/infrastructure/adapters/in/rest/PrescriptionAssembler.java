package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest;

import io.quarkus.hal.HalEntityWrapper;
import io.quarkus.hal.HalLink;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.core.UriInfo;
import net.fmjaeschke.quantumhealth.domain.model.Permission;
import net.fmjaeschke.quantumhealth.application.ports.out.AccessPolicy;
import net.fmjaeschke.quantumhealth.domain.model.Prescription;
import net.fmjaeschke.quantumhealth.domain.model.PrescriptionPage;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.PrescriptionListResponse;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.PrescriptionResponse;

import java.util.LinkedHashMap;

@RequestScoped
public class PrescriptionAssembler {

    private final AccessPolicy accessPolicy;

    public PrescriptionAssembler(AccessPolicy accessPolicy) {
        this.accessPolicy = accessPolicy;
    }

    public HalEntityWrapper<PrescriptionResponse> toHal(Prescription prescription, UserId actor, UriInfo uriInfo) {
        var response = toResponse(prescription);
        var selfUri = uriInfo.getBaseUriBuilder()
                .path("prescriptions")
                .path(prescription.getId().value().toString())
                .build()
                .toString();

        var links = new LinkedHashMap<String, HalLink>();
        links.put("self", new HalLink(selfUri, null, null));

        if (prescription.isFulfillable() && accessPolicy.isAllowed(Permission.DISPENSE_MEDICATION, actor)) {
            links.put("fulfill-prescription", new HalLink(selfUri + "/fulfill", null, null));
        }
        if (prescription.isCancellable()
                && accessPolicy.isAllowed(Permission.CANCEL_PRESCRIPTION, actor)
                && accessPolicy.mayAccessOwnedBy(prescription.getDoctorId(), actor)) {
            links.put("cancel-prescription", new HalLink(selfUri + "/cancel", null, null));
        }

        return new HalEntityWrapper<>(response, links);
    }

    public PrescriptionListResponse toListResponse(PrescriptionPage page, UriInfo uriInfo) {
        var responses = page.prescriptions().stream().map(this::toResponse).toList();
        var selfUri = uriInfo.getAbsolutePath().toString();
        return new PrescriptionListResponse(
                new PrescriptionListResponse.Embedded(responses),
                new PrescriptionListResponse.Links(new PrescriptionListResponse.Link(selfUri)),
                page.page(), page.pageSize(), page.totalElements());
    }

    private PrescriptionResponse toResponse(Prescription p) {
        return new PrescriptionResponse(
                p.getId().value(),
                p.getPatientId().value(),
                p.getPatientName(),
                p.getDoctorId().value(),
                p.getDoctorName(),
                p.getMedications(),
                p.getStatus(),
                p.getIssuedAt(),
                p.getFulfilledAt(),
                p.getFulfilledBy() != null ? p.getFulfilledBy().value() : null,
                p.getCancelledAt(),
                p.getCancelledBy() != null ? p.getCancelledBy().value() : null,
                p.getCancelledReason(),
                p.getExpiredAt());
    }
}
