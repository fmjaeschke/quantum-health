package net.fmjaeschke.quantumhealth.infrastructure.adapters.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PatientPageResponse(@JsonProperty("_embedded") Embedded embedded, @JsonProperty("_links") Links links,
                                  long totalElements, int totalPages, int page, int size) {
    public record Embedded(List<PatientResponse> patients) {
    }

    public record Links(Link self, Link first, Link last, @JsonInclude(JsonInclude.Include.NON_NULL) Link next,
                        @JsonInclude(JsonInclude.Include.NON_NULL) Link prev) {
    }

    public record Link(String href) {
    }
}
