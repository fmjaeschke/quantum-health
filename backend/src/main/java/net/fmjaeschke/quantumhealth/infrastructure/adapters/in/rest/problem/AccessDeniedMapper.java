package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.problem;

import io.quarkiverse.resteasy.problem.HttpProblem;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import net.fmjaeschke.quantumhealth.application.exception.AccessDeniedException;

@Provider
public class AccessDeniedMapper implements ExceptionMapper<AccessDeniedException> {

    @Override
    public Response toResponse(AccessDeniedException e) {
        return HttpProblem.builder()
                .withStatus(Status.FORBIDDEN)
                .withTitle("Forbidden")
                .withDetail(e.getMessage())
                .build()
                .toResponse();
    }
}
