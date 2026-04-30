package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest;

import io.quarkus.hal.HalEntityWrapper;
import io.quarkus.security.identity.SecurityIdentity;
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
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import net.fmjaeschke.quantumhealth.application.ports.in.CancelAppointmentUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ConfirmAppointmentUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ListAppointmentsUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ReadAppointmentUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ScheduleAppointmentUseCase;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentId;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.AppointmentListResponse;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.AppointmentResponse;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.ScheduleAppointmentRequest;

import java.util.UUID;

@Path("/appointments")
@RequestScoped
@Produces({MediaType.APPLICATION_JSON, "application/hal+json"})
public class AppointmentResource {

    private final ScheduleAppointmentUseCase scheduleAppointment;
    private final ReadAppointmentUseCase readAppointment;
    private final ListAppointmentsUseCase listAppointments;
    private final ConfirmAppointmentUseCase confirmAppointment;
    private final CancelAppointmentUseCase cancelAppointment;
    private final SecurityIdentity identity;
    private final AppointmentAssembler assembler;

    @Context
    UriInfo uriInfo;

    public AppointmentResource(ScheduleAppointmentUseCase scheduleAppointment,
                                ReadAppointmentUseCase readAppointment,
                                ListAppointmentsUseCase listAppointments,
                                ConfirmAppointmentUseCase confirmAppointment,
                                CancelAppointmentUseCase cancelAppointment,
                                SecurityIdentity identity,
                                AppointmentAssembler assembler) {
        this.scheduleAppointment = scheduleAppointment;
        this.readAppointment = readAppointment;
        this.listAppointments = listAppointments;
        this.confirmAppointment = confirmAppointment;
        this.cancelAppointment = cancelAppointment;
        this.identity = identity;
        this.assembler = assembler;
    }

    @POST
    @RolesAllowed({"CLERK", "DOCTOR", "ADMIN"})
    @Consumes(MediaType.APPLICATION_JSON)
    public Response schedule(@Valid ScheduleAppointmentRequest request) {
        var actor = UserId.of(identity.getPrincipal().getName());
        var appointment = scheduleAppointment.schedule(
                actor,
                PatientId.of(request.patientId()),
                request.patientName(),
                UserId.of(request.doctorId()),
                request.doctorName(),
                request.scheduledAt());
        var location = uriInfo.getAbsolutePathBuilder()
                .path(appointment.getId().value().toString())
                .build();
        return Response.created(location)
                .entity(assembler.toHal(appointment, uriInfo))
                .build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"CLERK", "DOCTOR", "NURSE", "ADMIN"})
    public HalEntityWrapper<AppointmentResponse> findById(@PathParam("id") UUID id) {
        var actor = UserId.of(identity.getPrincipal().getName());
        var appointment = readAppointment.findById(AppointmentId.of(id), actor);
        return assembler.toHal(appointment, uriInfo);
    }

    @GET
    @RolesAllowed({"CLERK", "DOCTOR", "NURSE", "ADMIN"})
    public AppointmentListResponse list() {
        var actor = UserId.of(identity.getPrincipal().getName());
        var appointments = identity.hasRole("DOCTOR")
                ? listAppointments.listByDoctor(actor, actor)
                : listAppointments.findAll(actor);
        return assembler.toListResponse(appointments, uriInfo);
    }

    @POST
    @Path("/{id}/confirm")
    @RolesAllowed({"CLERK", "DOCTOR", "ADMIN"})
    public HalEntityWrapper<AppointmentResponse> confirm(@PathParam("id") UUID id) {
        var actor = UserId.of(identity.getPrincipal().getName());
        var appointment = confirmAppointment.confirm(AppointmentId.of(id), actor);
        return assembler.toHal(appointment, uriInfo);
    }

    @POST
    @Path("/{id}/cancel")
    @RolesAllowed({"CLERK", "DOCTOR", "NURSE", "ADMIN"})
    public HalEntityWrapper<AppointmentResponse> cancel(@PathParam("id") UUID id) {
        var actor = UserId.of(identity.getPrincipal().getName());
        var appointment = cancelAppointment.cancel(AppointmentId.of(id), actor);
        return assembler.toHal(appointment, uriInfo);
    }
}
