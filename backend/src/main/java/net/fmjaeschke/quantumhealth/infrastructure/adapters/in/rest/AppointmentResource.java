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
import net.fmjaeschke.quantumhealth.application.ports.in.CancelAppointmentUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.CheckInUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ConfirmAppointmentUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ListAppointmentsUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ReadAppointmentUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ScheduleAppointmentUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.StartEncounterUseCase;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentId;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentQuery;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentStatus;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

import java.util.Optional;
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
    private final CheckInUseCase checkIn;
    private final StartEncounterUseCase startEncounter;
    private final UserId actor;
    private final AppointmentAssembler assembler;

    @Context
    UriInfo uriInfo;

    public AppointmentResource(ScheduleAppointmentUseCase scheduleAppointment,
                                ReadAppointmentUseCase readAppointment,
                                ListAppointmentsUseCase listAppointments,
                                ConfirmAppointmentUseCase confirmAppointment,
                                CancelAppointmentUseCase cancelAppointment,
                                CheckInUseCase checkIn,
                                StartEncounterUseCase startEncounter,
                                UserId actor,
                                AppointmentAssembler assembler) {
        this.scheduleAppointment = scheduleAppointment;
        this.readAppointment = readAppointment;
        this.listAppointments = listAppointments;
        this.confirmAppointment = confirmAppointment;
        this.cancelAppointment = cancelAppointment;
        this.checkIn = checkIn;
        this.startEncounter = startEncounter;
        this.actor = actor;
        this.assembler = assembler;
    }

    @POST
    @RolesAllowed({"CLERK", "ADMIN"})
    @Consumes(MediaType.APPLICATION_JSON)
    public Response schedule(@Valid ScheduleAppointmentRequest request) {
        var appointment = scheduleAppointment.schedule(
                actor,
                PatientId.of(request.patientId()),
                UserId.of(request.doctorId()),
                request.scheduledAt(),
                request.reason());
        var location = uriInfo.getAbsolutePathBuilder()
                .path(appointment.getId().value().toString())
                .build();
        return Response.created(location)
                .entity(assembler.toHal(appointment, actor, uriInfo))
                .build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"CLERK", "DOCTOR", "NURSE", "ADMIN"})
    public HalEntityWrapper<AppointmentResponse> findById(@PathParam("id") UUID id) {
        var appointment = readAppointment.findById(AppointmentId.of(id), actor);
        return assembler.toHal(appointment, actor, uriInfo);
    }

    @GET
    @RolesAllowed({"CLERK", "DOCTOR", "NURSE", "ADMIN"})
    public AppointmentListResponse list(
            @QueryParam("status") AppointmentStatus status,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        var query = new AppointmentQuery(Optional.ofNullable(status), Optional.empty(), page, size);
        var appointmentPage = listAppointments.list(query, actor);
        return assembler.toListResponse(appointmentPage, uriInfo);
    }

    @POST
    @Path("/{id}/confirm")
    @RolesAllowed({"CLERK", "ADMIN"})
    public HalEntityWrapper<AppointmentResponse> confirm(@PathParam("id") UUID id) {
        var appointment = confirmAppointment.confirm(AppointmentId.of(id), actor);
        return assembler.toHal(appointment, actor, uriInfo);
    }

    @POST
    @Path("/{id}/check-in")
    @RolesAllowed({"CLERK", "ADMIN"})
    public HalEntityWrapper<AppointmentResponse> checkIn(@PathParam("id") UUID id) {
        var appointment = checkIn.checkIn(AppointmentId.of(id), actor);
        return assembler.toHal(appointment, actor, uriInfo);
    }

    @POST
    @Path("/{id}/start")
    @RolesAllowed({"DOCTOR"})
    public HalEntityWrapper<AppointmentResponse> start(@PathParam("id") UUID id) {
        var appointment = startEncounter.start(AppointmentId.of(id), actor);
        return assembler.toHal(appointment, actor, uriInfo);
    }

    @POST
    @Path("/{id}/cancel")
    @RolesAllowed({"CLERK", "DOCTOR", "ADMIN"})
    public HalEntityWrapper<AppointmentResponse> cancel(@PathParam("id") UUID id) {
        var appointment = cancelAppointment.cancel(AppointmentId.of(id), actor);
        return assembler.toHal(appointment, actor, uriInfo);
    }
}
