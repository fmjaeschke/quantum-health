package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest;

import io.quarkus.hal.HalEntityWrapper;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import net.fmjaeschke.quantumhealth.application.ports.in.AddClinicalNoteUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.CompleteEncounterUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ReadEncounterUseCase;
import net.fmjaeschke.quantumhealth.domain.model.EncounterId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.AddClinicalNoteRequest;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.EncounterResponse;

import java.util.UUID;

@Path("/encounters")
@RequestScoped
@Produces({MediaType.APPLICATION_JSON, "application/hal+json"})
public class EncounterResource {

    private final ReadEncounterUseCase readEncounter;
    private final AddClinicalNoteUseCase addClinicalNote;
    private final CompleteEncounterUseCase completeEncounter;
    private final UserId actor;
    private final EncounterAssembler assembler;

    @Context
    UriInfo uriInfo;

    public EncounterResource(ReadEncounterUseCase readEncounter, AddClinicalNoteUseCase addClinicalNote,
                             CompleteEncounterUseCase completeEncounter, UserId actor, EncounterAssembler assembler) {
        this.readEncounter = readEncounter;
        this.addClinicalNote = addClinicalNote;
        this.completeEncounter = completeEncounter;
        this.actor = actor;
        this.assembler = assembler;
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"DOCTOR", "ADMIN"})
    public HalEntityWrapper<EncounterResponse> findById(@PathParam("id") UUID id) {
        var encounter = readEncounter.findById(EncounterId.of(id), actor);
        return assembler.toHal(encounter, actor, uriInfo);
    }

    @POST
    @Path("/{id}/notes")
    @RolesAllowed("DOCTOR")
    @Consumes(MediaType.APPLICATION_JSON)
    public HalEntityWrapper<EncounterResponse> addNote(@PathParam("id") UUID id, @Valid AddClinicalNoteRequest request) {
        var encounter = addClinicalNote.addNote(EncounterId.of(id), request.content(), actor);
        return assembler.toHal(encounter, actor, uriInfo);
    }

    @POST
    @Path("/{id}/complete")
    @RolesAllowed("DOCTOR")
    public HalEntityWrapper<EncounterResponse> complete(@PathParam("id") UUID id) {
        var encounter = completeEncounter.complete(EncounterId.of(id), actor);
        return assembler.toHal(encounter, actor, uriInfo);
    }
}
