package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AppointmentListResponse(
        @JsonProperty("_embedded") Embedded embedded,
        @JsonProperty("_links") Links links
) {
    public record Embedded(@JsonProperty("appointments") List<AppointmentResponse> appointments) {}
    public record Links(Link self) {}
    public record Link(String href) {}
}
