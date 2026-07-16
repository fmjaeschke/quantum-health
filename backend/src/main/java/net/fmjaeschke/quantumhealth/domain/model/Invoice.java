package net.fmjaeschke.quantumhealth.domain.model;

import net.fmjaeschke.quantumhealth.domain.exception.InvalidInvoiceStateException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class Invoice {

    private final InvoiceId id;
    private final EncounterId encounterId;
    private final PatientId patientId;
    private final BigDecimal totalAmount;
    private final BigDecimal insurerAmount;
    private final BigDecimal patientCopay;
    private final InvoiceStatus status;
    private final Instant patientPaidAt;

    private Invoice(InvoiceId id, EncounterId encounterId, PatientId patientId, BigDecimal totalAmount,
                    BigDecimal insurerAmount, BigDecimal patientCopay, InvoiceStatus status, Instant patientPaidAt) {
        this.id            = Objects.requireNonNull(id,            "id");
        this.encounterId   = Objects.requireNonNull(encounterId,   "encounterId");
        this.patientId     = Objects.requireNonNull(patientId,     "patientId");
        this.totalAmount   = Objects.requireNonNull(totalAmount,   "totalAmount");
        this.insurerAmount = Objects.requireNonNull(insurerAmount, "insurerAmount");
        this.patientCopay  = Objects.requireNonNull(patientCopay,  "patientCopay");
        this.status        = Objects.requireNonNull(status,        "status");
        this.patientPaidAt = patientPaidAt;
    }

    public static Invoice draft(EncounterId encounterId, PatientId patientId, BigDecimal totalAmount) {
        return new Invoice(InvoiceId.generate(), encounterId, patientId, totalAmount,
                BigDecimal.ZERO, BigDecimal.ZERO, InvoiceStatus.DRAFT, null);
    }

    public static Invoice reconstitute(InvoiceId id, EncounterId encounterId, PatientId patientId,
                                       BigDecimal totalAmount, BigDecimal insurerAmount, BigDecimal patientCopay,
                                       InvoiceStatus status, Instant patientPaidAt) {
        return new Invoice(id, encounterId, patientId, totalAmount, insurerAmount, patientCopay, status, patientPaidAt);
    }

    /**
     * Pure split calculation: a missing policy defaults to 0% coverage (patient pays 100%),
     * rather than being treated as an error.
     */
    public Invoice calculateSplit(InsurancePolicy policy) {
        var coverageRate = policy == null ? BigDecimal.ZERO : policy.tier().coverageRate();
        var insurer = totalAmount.multiply(coverageRate).setScale(2, RoundingMode.HALF_UP);
        var copay = totalAmount.subtract(insurer);
        return new Invoice(id, encounterId, patientId, totalAmount, insurer, copay, status, patientPaidAt);
    }

    public Invoice submit() {
        return new Invoice(id, encounterId, patientId, totalAmount, insurerAmount, patientCopay,
                InvoiceStatus.SUBMITTED, patientPaidAt);
    }

    public Invoice pay() {
        if (!isPayable()) {
            throw new InvalidInvoiceStateException("pay", status);
        }
        return new Invoice(id, encounterId, patientId, totalAmount, insurerAmount, patientCopay,
                InvoiceStatus.PAID, patientPaidAt);
    }

    public Invoice deny() {
        if (!isDeniable()) {
            throw new InvalidInvoiceStateException("deny", status);
        }
        return new Invoice(id, encounterId, patientId, totalAmount, insurerAmount, patientCopay,
                InvoiceStatus.DENIED, patientPaidAt);
    }

    public Invoice appeal() {
        if (!isAppealable()) {
            throw new InvalidInvoiceStateException("appeal", status);
        }
        return new Invoice(id, encounterId, patientId, totalAmount, insurerAmount, patientCopay,
                InvoiceStatus.APPEALED, patientPaidAt);
    }

    public Invoice processPatientPayment() {
        if (!isPatientPaymentProcessable()) {
            throw new InvalidInvoiceStateException("process-patient-payment", status);
        }
        return new Invoice(id, encounterId, patientId, totalAmount, insurerAmount, patientCopay,
                status, Instant.now());
    }

    public boolean isPayable()     { return status == InvoiceStatus.SUBMITTED; }
    public boolean isDeniable()    { return status == InvoiceStatus.SUBMITTED; }
    public boolean isAppealable()  { return status == InvoiceStatus.DENIED; }
    public boolean isPatientPaymentProcessable() { return patientPaidAt == null; }

    public InvoiceId getId()               { return id; }
    public EncounterId getEncounterId()    { return encounterId; }
    public PatientId getPatientId()        { return patientId; }
    public BigDecimal getTotalAmount()     { return totalAmount; }
    public BigDecimal getInsurerAmount()   { return insurerAmount; }
    public BigDecimal getPatientCopay()    { return patientCopay; }
    public InvoiceStatus getStatus()       { return status; }
    public Optional<Instant> getPatientPaidAt() { return Optional.ofNullable(patientPaidAt); }
}
