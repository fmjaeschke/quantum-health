package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest;

import io.quarkus.hal.HalEntityWrapper;
import io.quarkus.hal.HalService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.core.UriInfo;
import net.fmjaeschke.quantumhealth.domain.model.Patient;
import net.fmjaeschke.quantumhealth.domain.model.PatientPage;
import net.fmjaeschke.quantumhealth.domain.model.PatientQuery;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.PatientPageResponse;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.PatientPageResponse.Embedded;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.PatientPageResponse.Link;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.PatientPageResponse.Links;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.PatientResponse;

@RequestScoped
public class PatientAssembler {

    private final HalService halService;

    public PatientAssembler(HalService halService) {
        this.halService = halService;
    }

    public HalEntityWrapper<PatientResponse> toHal(Patient patient) {
        return halService.toHalWrapper(new PatientResponse(patient.getId()
                .value(), patient.getFirstName(), patient.getLastName(), patient.getDateOfBirth()));
    }

    public PatientPageResponse toPageResponse(PatientPage page, UriInfo uriInfo, PatientQuery query) {
        var patients = page.patients()
                .stream()
                .map(p -> new PatientResponse(p.getId()
                        .value(), p.getFirstName(), p.getLastName(), p.getDateOfBirth()))
                .toList();

        int totalPages = (int) Math.ceil((double) page.totalElements() / page.size());

        var self = pageLink(uriInfo, query, page.page());
        var first = pageLink(uriInfo, query, 0);
        var last = pageLink(uriInfo, query, Math.max(0, totalPages - 1));
        var next = page.page() < totalPages - 1 ? pageLink(uriInfo, query, page.page() + 1) : null;
        var prev = page.page() > 0 ? pageLink(uriInfo, query, page.page() - 1) : null;

        return new PatientPageResponse(new Embedded(patients), new Links(self, first, last, next, prev), page.totalElements(), totalPages, page.page(), page.size());
    }

    private Link pageLink(UriInfo uriInfo, PatientQuery query, int targetPage) {
        var builder = uriInfo.getAbsolutePathBuilder()
                .queryParam("page", targetPage)
                .queryParam("size", query.size())
                .queryParam("sort", query.sortField())
                .queryParam("direction", query.sortDirection());
        query.search()
                .ifPresent(s -> builder.queryParam("search", s));
        query.dateOfBirth()
                .ifPresent(d -> builder.queryParam("dateOfBirth", d));
        return new Link(builder.build()
                .toString());
    }
}
