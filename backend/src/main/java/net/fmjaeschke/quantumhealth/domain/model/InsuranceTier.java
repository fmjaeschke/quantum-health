package net.fmjaeschke.quantumhealth.domain.model;

import java.math.BigDecimal;

public enum InsuranceTier {
    GOLD(new BigDecimal("0.90")),
    SILVER(new BigDecimal("0.70")),
    BRONZE(new BigDecimal("0.50"));

    private final BigDecimal coverageRate;

    InsuranceTier(BigDecimal coverageRate) {
        this.coverageRate = coverageRate;
    }

    public BigDecimal coverageRate() {
        return coverageRate;
    }
}
