package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.problem;

import io.quarkiverse.resteasy.problem.HttpProblem;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import net.fmjaeschke.quantumhealth.domain.exception.EncounterCompletedException;

@Provider
public class EncounterCompletedExceptionMapper implements ExceptionMapper<EncounterCompletedException> {
    @Override
    public Response toResponse(EncounterCompletedException e) {
        return HttpProblem.builder()
                .withStatus(Response.Status.CONFLICT)
                .withTitle("Conflict")
                .withDetail(e.getMessage())
                .build()
                .toResponse();
    }
}
