package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.problem;

import io.quarkiverse.resteasy.problem.HttpProblem;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import net.fmjaeschke.quantumhealth.application.exception.PatientNotFoundException;

@Provider
public class PatientNotFoundMapper implements ExceptionMapper<PatientNotFoundException> {

    @Override
    public Response toResponse(PatientNotFoundException e) {
        return HttpProblem.builder()
                .withStatus(Status.NOT_FOUND)
                .withTitle("Not Found")
                .withDetail(e.getMessage())
                .build()
                .toResponse();
    }
}
