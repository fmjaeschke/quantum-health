package net.fmjaeschke.quantumhealth.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import net.fmjaeschke.quantumhealth.application.exception.ConcurrentModificationException;
import net.fmjaeschke.quantumhealth.application.exception.DoctorNotFoundException;
import net.fmjaeschke.quantumhealth.application.exception.PatientNotFoundException;
import net.fmjaeschke.quantumhealth.application.exception.PrescriptionNotFoundException;
import net.fmjaeschke.quantumhealth.application.ports.in.CancelPrescriptionUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ExpirePrescriptionsUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.FulfillPrescriptionUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.IssuePrescriptionUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ListPrescriptionsUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ReadPrescriptionUseCase;
import net.fmjaeschke.quantumhealth.application.ports.out.AccessPolicy;
import net.fmjaeschke.quantumhealth.application.ports.out.DoctorPort;
import net.fmjaeschke.quantumhealth.application.ports.out.PatientRepository;
import net.fmjaeschke.quantumhealth.application.ports.out.PrescriptionRepository;
import net.fmjaeschke.quantumhealth.domain.model.MedicationItem;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.Permission;
import net.fmjaeschke.quantumhealth.domain.model.Prescription;
import net.fmjaeschke.quantumhealth.domain.model.PrescriptionId;
import net.fmjaeschke.quantumhealth.domain.model.PrescriptionPage;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import org.jboss.logging.Logger;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Transactional
public class PrescriptionService implements IssuePrescriptionUseCase, ReadPrescriptionUseCase,
        FulfillPrescriptionUseCase, CancelPrescriptionUseCase, ListPrescriptionsUseCase,
        ExpirePrescriptionsUseCase {

    private static final Logger LOG = Logger.getLogger(PrescriptionService.class);

    private final PrescriptionRepository repository;
    private final PatientRepository patientRepository;
    private final DoctorPort doctorPort;
    private final AccessPolicy accessPolicy;
    private final Clock clock;

    public PrescriptionService(PrescriptionRepository repository,
                               PatientRepository patientRepository,
                               DoctorPort doctorPort,
                               AccessPolicy accessPolicy,
                               Clock clock) {
        this.repository = repository;
        this.patientRepository = patientRepository;
        this.doctorPort = doctorPort;
        this.accessPolicy = accessPolicy;
        this.clock = clock;
    }

    @Override
    public Prescription issue(UserId actor, PatientId patientId, List<MedicationItem> medications) {
        var patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException(patientId));
        var doctor = doctorPort.findById(actor)
                .orElseThrow(() -> new DoctorNotFoundException(actor));
        return repository.saveNew(
                Prescription.issue(patientId, patient.getFullName(), actor, doctor.displayName(), medications,
                        clock.instant()));
    }

    @Override
    public Prescription findById(PrescriptionId id, UserId actor) {
        var prescription = repository.findById(id)
                .orElseThrow(() -> new PrescriptionNotFoundException(id));
        if (!accessPolicy.mayAccessOwnedBy(prescription.getDoctorId(), actor)) {
            throw new PrescriptionNotFoundException(id);
        }
        return prescription;
    }

    @Override
    public Prescription fulfill(PrescriptionId id, UserId actor) {
        accessPolicy.check(Permission.DISPENSE_MEDICATION, actor, id);
        var prescription = repository.findById(id)
                .orElseThrow(() -> new PrescriptionNotFoundException(id));
        return repository.save(prescription.fulfill(actor, clock.instant()));
    }

    @Override
    public Prescription cancel(PrescriptionId id, UserId actor, String reason) {
        accessPolicy.check(Permission.CANCEL_PRESCRIPTION, actor, id);
        var prescription = repository.findById(id)
                .orElseThrow(() -> new PrescriptionNotFoundException(id));
        return repository.save(prescription.cancel(actor, reason, clock.instant()));
    }

    @Override
    public PrescriptionPage list(int page, int pageSize, UserId actor) {
        var doctorScope = accessPolicy.isDoctor() ? Optional.of(actor) : Optional.<UserId>empty();
        return repository.findAll(page, pageSize, doctorScope);
    }

    @Override
    public int expireOlderThan(Instant threshold) {
        var stale = repository.findStale(threshold);
        int expiredCount = 0;
        for (var prescription : stale) {
            try {
                repository.expireOne(prescription, clock.instant());
                expiredCount++;
            } catch (ConcurrentModificationException _) {
                // Unlike fulfill()/cancel(), this is a best-effort batch job: skip and retry
                // next run rather than letting the conflict abort the whole expiry pass.
                LOG.warnf("Skipping prescription %s during expiry: concurrently modified",
                        prescription.getId().value());
            }
        }
        return expiredCount;
    }
}
