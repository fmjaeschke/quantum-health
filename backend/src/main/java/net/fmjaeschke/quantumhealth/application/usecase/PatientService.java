package net.fmjaeschke.quantumhealth.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import net.fmjaeschke.quantumhealth.application.exception.PatientNotFoundException;
import net.fmjaeschke.quantumhealth.application.ports.in.ListPatientUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ReadPatientUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.RegisterPatientUseCase;
import net.fmjaeschke.quantumhealth.application.ports.out.AccessPolicy;
import net.fmjaeschke.quantumhealth.application.ports.out.PatientRepository;
import net.fmjaeschke.quantumhealth.domain.model.Patient;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.PatientPage;
import net.fmjaeschke.quantumhealth.domain.model.PatientQuery;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

import java.time.LocalDate;

@ApplicationScoped
@Transactional
public class PatientService implements RegisterPatientUseCase, ReadPatientUseCase, ListPatientUseCase {

    private final PatientRepository patients;
    private final AccessPolicy accessPolicy;

    @Inject
    public PatientService(PatientRepository patients, AccessPolicy accessPolicy) {
        this.patients = patients;
        this.accessPolicy = accessPolicy;
    }

    @Override
    public Patient register(UserId actor, String firstName, String lastName, LocalDate dateOfBirth) {
        var patient = Patient.register(firstName, lastName, dateOfBirth);
        patients.save(patient);
        return patient;
    }

    @Override
    public Patient findById(PatientId patientId, UserId actor) {
        var patient = patients.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException(patientId));
        if (!accessPolicy.mayAccessPatient(actor, patientId)) {
            throw new PatientNotFoundException(patientId);
        }
        return patient;
    }

    @Override
    public PatientPage listPatients(UserId actor, PatientQuery query) {
        if (accessPolicy.isDoctor()) {
            return patients.findByDoctor(actor, query);
        }
        return patients.findAll(query);
    }
}
