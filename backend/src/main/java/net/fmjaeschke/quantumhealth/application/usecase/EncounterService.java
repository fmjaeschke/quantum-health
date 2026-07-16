package net.fmjaeschke.quantumhealth.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import net.fmjaeschke.quantumhealth.application.exception.AppointmentNotFoundException;
import net.fmjaeschke.quantumhealth.application.exception.EncounterNotFoundException;
import net.fmjaeschke.quantumhealth.application.ports.in.AddClinicalNoteUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.CompleteEncounterUseCase;
import net.fmjaeschke.quantumhealth.application.ports.in.ReadEncounterUseCase;
import net.fmjaeschke.quantumhealth.application.ports.out.AccessPolicy;
import net.fmjaeschke.quantumhealth.application.ports.out.AppointmentRepository;
import net.fmjaeschke.quantumhealth.application.ports.out.DomainEventPublisher;
import net.fmjaeschke.quantumhealth.application.ports.out.EncounterNoteRepository;
import net.fmjaeschke.quantumhealth.application.ports.out.EncounterRepository;
import net.fmjaeschke.quantumhealth.domain.model.Encounter;
import net.fmjaeschke.quantumhealth.domain.model.EncounterCompletedEvent;
import net.fmjaeschke.quantumhealth.domain.model.EncounterId;
import net.fmjaeschke.quantumhealth.domain.model.Permission;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

@ApplicationScoped
@Transactional
public class EncounterService implements ReadEncounterUseCase, AddClinicalNoteUseCase, CompleteEncounterUseCase {

    private final EncounterRepository repository;
    private final AccessPolicy accessPolicy;
    private final EncounterNoteRepository noteRepository;
    private final AppointmentRepository appointmentRepository;
    private final DomainEventPublisher domainEventPublisher;

    public EncounterService(EncounterRepository repository, AccessPolicy accessPolicy,
                            EncounterNoteRepository noteRepository, AppointmentRepository appointmentRepository,
                            DomainEventPublisher domainEventPublisher) {
        this.repository = repository;
        this.accessPolicy = accessPolicy;
        this.noteRepository = noteRepository;
        this.appointmentRepository = appointmentRepository;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Override
    public Encounter findById(EncounterId id, UserId actor) {
        var encounter = repository.findById(id)
                .orElseThrow(() -> new EncounterNotFoundException(id));
        if (!accessPolicy.mayAccessPatient(actor, encounter.getPatientId())) {
            throw new EncounterNotFoundException(id);
        }
        return encounter.withNotes(noteRepository.findByEncounterId(id));
    }

    @Override
    public Encounter addNote(EncounterId id, String content, UserId actor) {
        accessPolicy.check(Permission.WRITE_ENCOUNTER, actor, id);
        var encounter = repository.findById(id)
                .orElseThrow(() -> new EncounterNotFoundException(id));
        var updated = encounter.withNotes(noteRepository.findByEncounterId(id)).addNote(content, actor);
        noteRepository.save(id, updated.getLatestNote().orElseThrow());
        return updated;
    }

    @Override
    public Encounter complete(EncounterId id, UserId actor) {
        accessPolicy.check(Permission.COMPLETE_ENCOUNTER, actor, id);
        var encounter = repository.findById(id)
                .orElseThrow(() -> new EncounterNotFoundException(id))
                .withNotes(noteRepository.findByEncounterId(id));
        var completed = repository.save(encounter.complete());

        var appointment = appointmentRepository.findById(completed.getAppointmentId())
                .orElseThrow(() -> new AppointmentNotFoundException(completed.getAppointmentId()));
        appointmentRepository.save(appointment.complete());

        domainEventPublisher.publish(new EncounterCompletedEvent(
                completed.getId(), completed.getAppointmentId(), completed.getPatientId(), completed.getDoctorId()));

        return completed;
    }
}
