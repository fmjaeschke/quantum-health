package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PrescriptionListResponse(
        @JsonProperty("_embedded") Embedded embedded,
        @JsonProperty("_links") Links links,
        int page,
        int pageSize,
        long totalElements
) {
    public record Embedded(@JsonProperty("prescriptions") List<PrescriptionResponse> prescriptions) {}
    public record Links(Link self) {}
    public record Link(String href) {}
}
