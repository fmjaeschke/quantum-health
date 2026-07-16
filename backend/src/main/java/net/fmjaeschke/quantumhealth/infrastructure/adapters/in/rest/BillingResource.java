package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest;

import io.quarkus.hal.HalEntityWrapper;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import net.fmjaeschke.quantumhealth.application.ports.in.AppealInvoiceUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.DenyInvoiceUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.PayInvoiceUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ProcessPatientPaymentUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ReadInvoiceUseCase;
import net.fmjaeschke.quantumhealth.domain.model.InvoiceId;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.InvoiceResponse;

import java.util.UUID;

@Path("/invoices")
@RequestScoped
@Produces({MediaType.APPLICATION_JSON, "application/hal+json", "application/vnd.quantumhealth.v1+json"})
public class BillingResource {

    private final ReadInvoiceUseCase readInvoice;
    private final PayInvoiceUseCase payInvoice;
    private final DenyInvoiceUseCase denyInvoice;
    private final AppealInvoiceUseCase appealInvoice;
    private final ProcessPatientPaymentUseCase processPatientPayment;
    private final InvoiceAssembler assembler;

    @Context
    UriInfo uriInfo;

    public BillingResource(ReadInvoiceUseCase readInvoice, PayInvoiceUseCase payInvoice,
                           DenyInvoiceUseCase denyInvoice, AppealInvoiceUseCase appealInvoice,
                           ProcessPatientPaymentUseCase processPatientPayment,
                           InvoiceAssembler assembler) {
        this.readInvoice = readInvoice;
        this.payInvoice = payInvoice;
        this.denyInvoice = denyInvoice;
        this.appealInvoice = appealInvoice;
        this.processPatientPayment = processPatientPayment;
        this.assembler = assembler;
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"CLERK", "ADMIN"})
    public HalEntityWrapper<InvoiceResponse> findById(@PathParam("id") UUID id) {
        var invoice = readInvoice.findById(InvoiceId.of(id));
        return assembler.toHal(invoice, uriInfo);
    }

    @POST
    @Path("/{id}/pay")
    @RolesAllowed({"CLERK", "ADMIN"})
    public HalEntityWrapper<InvoiceResponse> pay(@PathParam("id") UUID id) {
        var invoice = payInvoice.pay(InvoiceId.of(id));
        return assembler.toHal(invoice, uriInfo);
    }

    @POST
    @Path("/{id}/deny")
    @RolesAllowed({"CLERK", "ADMIN"})
    public HalEntityWrapper<InvoiceResponse> deny(@PathParam("id") UUID id) {
        var invoice = denyInvoice.deny(InvoiceId.of(id));
        return assembler.toHal(invoice, uriInfo);
    }

    @POST
    @Path("/{id}/appeal")
    @RolesAllowed({"CLERK", "ADMIN"})
    public HalEntityWrapper<InvoiceResponse> appeal(@PathParam("id") UUID id) {
        var invoice = appealInvoice.appeal(InvoiceId.of(id));
        return assembler.toHal(invoice, uriInfo);
    }

    @POST
    @Path("/{id}/process-patient-payment")
    @RolesAllowed({"CLERK", "ADMIN"})
    public HalEntityWrapper<InvoiceResponse> processPatientPayment(@PathParam("id") UUID id) {
        var invoice = processPatientPayment.processPatientPayment(InvoiceId.of(id));
        return assembler.toHal(invoice, uriInfo);
    }
}
