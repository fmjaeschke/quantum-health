package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.problem;

import io.quarkiverse.resteasy.problem.HttpProblem;
import jakarta.persistence.OptimisticLockException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class OptimisticLockExceptionMapper implements ExceptionMapper<OptimisticLockException> {

    @Override
    public Response toResponse(OptimisticLockException e) {
        return HttpProblem.builder()
                .withStatus(Status.CONFLICT)
                .withTitle("Conflict")
                .withDetail("The resource was modified concurrently. Please retry with the latest version.")
                .build()
                .toResponse();
    }
}
