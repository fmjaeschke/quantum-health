package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.problem;

import io.quarkiverse.resteasy.problem.HttpProblem;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import net.fmjaeschke.quantumhealth.application.exception.DuplicateAppointmentException;

@Provider
public class DuplicateAppointmentMapper implements ExceptionMapper<DuplicateAppointmentException> {

    @Override
    public Response toResponse(DuplicateAppointmentException e) {
        return HttpProblem.builder()
                .withStatus(Status.CONFLICT)
                .withTitle("Conflict")
                .withDetail(e.getMessage())
                .build()
                .toResponse();
    }
}
