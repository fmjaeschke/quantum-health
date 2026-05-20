package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest;

import io.quarkus.hal.HalEntityWrapper;
import io.quarkus.resteasy.reactive.links.InjectRestLinks;
import io.quarkus.resteasy.reactive.links.RestLink;
import io.quarkus.resteasy.reactive.links.RestLinkType;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import net.fmjaeschke.quantumhealth.application.ports.in.ListPatientUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ReadPatientUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.RegisterPatientUseCase;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.PatientQuery;
import net.fmjaeschke.quantumhealth.domain.model.PatientQuery.SortDirection;
import net.fmjaeschke.quantumhealth.domain.model.PatientQuery.SortField;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.PatientPageResponse;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.PatientResponse;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.RegisterPatientRequest;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Path("/patients")
@RequestScoped
@Produces({MediaType.APPLICATION_JSON, "application/hal+json"})
@Consumes(MediaType.APPLICATION_JSON)
public class PatientResource {

    private final RegisterPatientUseCase registerPatient;
    private final ReadPatientUseCase readPatient;
    private final ListPatientUseCase listPatients;
    private final UserId actor;
    private final PatientAssembler assembler;

    @Context
    UriInfo uriInfo;

    public PatientResource(RegisterPatientUseCase registerPatient, ReadPatientUseCase readPatient,
                           ListPatientUseCase listPatients, UserId actor,
                           PatientAssembler assembler) {
        this.registerPatient = registerPatient;
        this.readPatient = readPatient;
        this.listPatients = listPatients;
        this.actor = actor;
        this.assembler = assembler;
    }

    @GET
    @RolesAllowed({"CLERK", "DOCTOR", "NURSE", "ADMIN"})
    public PatientPageResponse list(@QueryParam("search") String search, @QueryParam("dateOfBirth") LocalDate dateOfBirth, @QueryParam("page") @DefaultValue("0") int page,
                                    @QueryParam("size") @DefaultValue("20") int size, @QueryParam("sort") @DefaultValue("LAST_NAME") SortField sortField,
                                    @QueryParam("direction") @DefaultValue("ASC") SortDirection sortDirection) {
        var query = new PatientQuery(Optional.ofNullable(search), Optional.ofNullable(dateOfBirth), page, size, sortField, sortDirection);
        return assembler.toPageResponse(listPatients.listPatients(actor, query), uriInfo, query);
    }

    @POST
    @RolesAllowed({"CLERK", "ADMIN"})
    @InjectRestLinks(RestLinkType.INSTANCE)
    public Response register(@Valid RegisterPatientRequest request) {
        var patient = registerPatient.register(actor, request.firstName(), request.lastName(), request.dateOfBirth());
        var location = uriInfo.getAbsolutePathBuilder()
                .path(patient.getId()
                        .value()
                        .toString())
                .build();
        return Response.created(location)
                .entity(assembler.toHal(patient))
                .build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"CLERK", "DOCTOR", "NURSE", "ADMIN"})
    @RestLink(rel = "self")
    @InjectRestLinks(RestLinkType.INSTANCE)
    public HalEntityWrapper<PatientResponse> findById(@PathParam("id") UUID id) {
        var patient = readPatient.findById(PatientId.of(id), actor);
        return assembler.toHal(patient);
    }
}
