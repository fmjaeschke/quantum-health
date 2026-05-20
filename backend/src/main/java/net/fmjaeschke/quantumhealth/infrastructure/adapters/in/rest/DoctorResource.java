package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import net.fmjaeschke.quantumhealth.application.ports.in.FindDoctorsUseCase;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.DoctorResponse;

import java.util.List;

@Path("/doctors")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class DoctorResource {

    private final FindDoctorsUseCase findDoctors;

    public DoctorResource(FindDoctorsUseCase findDoctors) {
        this.findDoctors = findDoctors;
    }

    @GET
    @RolesAllowed({"CLERK", "ADMIN"})
    public List<DoctorResponse> list() {
        return findDoctors.findDoctors().stream()
                .map(d -> new DoctorResponse(d.id().value(), d.displayName()))
                .toList();
    }
}
