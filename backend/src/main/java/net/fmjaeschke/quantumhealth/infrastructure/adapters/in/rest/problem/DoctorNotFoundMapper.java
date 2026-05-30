package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.problem;

import io.quarkiverse.resteasy.problem.HttpProblem;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import net.fmjaeschke.quantumhealth.application.exception.DoctorNotFoundException;

@Provider
public class DoctorNotFoundMapper implements ExceptionMapper<DoctorNotFoundException> {

    @Override
    public Response toResponse(DoctorNotFoundException e) {
        return HttpProblem.builder()
                .withStatus(Status.NOT_FOUND)
                .withTitle("Not Found")
                .withDetail(e.getMessage())
                .build()
                .toResponse();
    }
}
