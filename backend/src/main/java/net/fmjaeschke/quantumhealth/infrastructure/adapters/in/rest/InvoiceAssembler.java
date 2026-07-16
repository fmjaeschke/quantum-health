package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest;

import io.quarkus.hal.HalEntityWrapper;
import io.quarkus.hal.HalLink;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.core.UriInfo;
import net.fmjaeschke.quantumhealth.domain.model.Invoice;
import net.fmjaeschke.quantumhealth.domain.model.InvoiceStatus;
import net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto.InvoiceResponse;

import java.util.LinkedHashMap;

@RequestScoped
public class InvoiceAssembler {

    public HalEntityWrapper<InvoiceResponse> toHal(Invoice invoice, UriInfo uriInfo) {
        var response = toResponse(invoice);
        var selfUri = uriInfo.getBaseUriBuilder()
                .path("invoices")
                .path(invoice.getId().value().toString())
                .build()
                .toString();

        var links = new LinkedHashMap<String, HalLink>();
        links.put("self", new HalLink(selfUri, null, null));

        if (invoice.getStatus() == InvoiceStatus.DENIED) {
            links.put("appeal-denial", new HalLink(selfUri + "/appeal", null, null));
        }

        if (invoice.getPatientCopay().signum() > 0 && invoice.getPatientPaidAt().isEmpty()) {
            links.put("process-patient-payment", new HalLink(selfUri + "/process-patient-payment", null, null));
        }

        return new HalEntityWrapper<>(response, links);
    }

    private InvoiceResponse toResponse(Invoice i) {
        return new InvoiceResponse(
                i.getId().value(),
                i.getEncounterId().value(),
                i.getPatientId().value(),
                i.getTotalAmount(),
                i.getInsurerAmount(),
                i.getPatientCopay(),
                i.getStatus(),
                i.getPatientPaidAt().orElse(null));
    }
}
