package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.problem;

import io.quarkiverse.resteasy.problem.HttpProblem;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import net.fmjaeschke.quantumhealth.domain.exception.InvalidInvoiceStateException;

@Provider
public class InvalidInvoiceStateExceptionMapper implements ExceptionMapper<InvalidInvoiceStateException> {
    @Override
    public Response toResponse(InvalidInvoiceStateException e) {
        return HttpProblem.builder()
                .withStatus(Response.Status.CONFLICT)
                .withTitle("Conflict")
                .withDetail(e.getMessage())
                .build()
                .toResponse();
    }
}
