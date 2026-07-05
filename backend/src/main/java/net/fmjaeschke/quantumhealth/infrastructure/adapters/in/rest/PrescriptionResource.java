package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest;

import io.quarkus.hal.HalEntityWrapper;
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
import net.fmjaeschke.quantumhealth.application.ports.in.CancelPrescriptionUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.FulfillPrescriptionUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.IssuePrescriptionUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ListPrescriptionsUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ReadPrescriptionUseCase;
import net.fmjaeschke.quantumhealth.domain.model.MedicationItem;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.PrescriptionId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.CancelPrescriptionRequest;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.IssuePrescriptionRequest;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.PrescriptionListResponse;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.PrescriptionResponse;

import java.util.UUID;

@Path("/prescriptions")
@RequestScoped
@Produces({MediaType.APPLICATION_JSON, "application/hal+json"})
public class PrescriptionResource {

    private final IssuePrescriptionUseCase issuePrescription;
    private final ReadPrescriptionUseCase readPrescription;
    private final ListPrescriptionsUseCase listPrescriptions;
    private final FulfillPrescriptionUseCase fulfillPrescription;
    private final CancelPrescriptionUseCase cancelPrescription;
    private final UserId actor;
    private final PrescriptionAssembler assembler;

    @Context
    UriInfo uriInfo;

    public PrescriptionResource(IssuePrescriptionUseCase issuePrescription,
                                ReadPrescriptionUseCase readPrescription,
                                ListPrescriptionsUseCase listPrescriptions,
                                FulfillPrescriptionUseCase fulfillPrescription,
                                CancelPrescriptionUseCase cancelPrescription,
                                UserId actor,
                                PrescriptionAssembler assembler) {
        this.issuePrescription = issuePrescription;
        this.readPrescription = readPrescription;
        this.listPrescriptions = listPrescriptions;
        this.fulfillPrescription = fulfillPrescription;
        this.cancelPrescription = cancelPrescription;
        this.actor = actor;
        this.assembler = assembler;
    }

    @POST
    @RolesAllowed("DOCTOR")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response issue(@Valid IssuePrescriptionRequest request) {
        var medications = request.medications().stream()
                .map(m -> new MedicationItem(m.drugName(), m.dosage(), m.frequency()))
                .toList();
        var prescription = issuePrescription.issue(actor, PatientId.of(request.patientId()), medications);
        var location = uriInfo.getAbsolutePathBuilder()
                .path(prescription.getId().value().toString())
                .build();
        return Response.created(location)
                .entity(assembler.toHal(prescription, actor, uriInfo))
                .build();
    }

    @GET
    @RolesAllowed({"PHARMACIST", "DOCTOR", "ADMIN"})
    public PrescriptionListResponse list(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        var prescriptionPage = listPrescriptions.list(page, size, actor);
        return assembler.toListResponse(prescriptionPage, uriInfo);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"PHARMACIST", "DOCTOR", "ADMIN"})
    public HalEntityWrapper<PrescriptionResponse> findById(@PathParam("id") UUID id) {
        var prescription = readPrescription.findById(PrescriptionId.of(id), actor);
        return assembler.toHal(prescription, actor, uriInfo);
    }

    @POST
    @Path("/{id}/fulfill")
    @RolesAllowed("PHARMACIST")
    public HalEntityWrapper<PrescriptionResponse> fulfill(@PathParam("id") UUID id) {
        var prescription = fulfillPrescription.fulfill(PrescriptionId.of(id), actor);
        return assembler.toHal(prescription, actor, uriInfo);
    }

    @POST
    @Path("/{id}/cancel")
    @RolesAllowed({"DOCTOR", "ADMIN"})
    @Consumes(MediaType.APPLICATION_JSON)
    public HalEntityWrapper<PrescriptionResponse> cancel(@PathParam("id") UUID id, @Valid CancelPrescriptionRequest request) {
        var prescription = cancelPrescription.cancel(PrescriptionId.of(id), actor, request.reason());
        return assembler.toHal(prescription, actor, uriInfo);
    }
}
