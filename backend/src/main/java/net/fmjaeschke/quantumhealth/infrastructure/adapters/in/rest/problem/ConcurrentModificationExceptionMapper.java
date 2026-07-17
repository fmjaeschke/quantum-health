package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.problem;

import io.quarkiverse.resteasy.problem.HttpProblem;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import net.fmjaeschke.quantumhealth.application.exception.ConcurrentModificationException;

@Provider
public class ConcurrentModificationExceptionMapper implements ExceptionMapper<ConcurrentModificationException> {

    @Override
    public Response toResponse(ConcurrentModificationException e) {
        return HttpProblem.builder()
                .withStatus(Status.CONFLICT)
                .withTitle("Conflict")
                .withDetail("The resource was modified concurrently. Please retry with the latest version.")
                .build()
                .toResponse();
    }
}
