package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.event;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import net.fmjaeschke.quantumhealth.application.ports.in.GenerateInvoiceUseCase;
import net.fmjaeschke.quantumhealth.application.ports.out.DomainEventPublisher;
import net.fmjaeschke.quantumhealth.domain.model.AppointmentId;
import net.fmjaeschke.quantumhealth.domain.model.EncounterCompletedEvent;
import net.fmjaeschke.quantumhealth.domain.model.EncounterId;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class CdiDomainEventPublisherTest {

    @Inject
    DomainEventPublisher publisher;

    @Inject
    RecordingEncounterCompletedObserver observer;

    // Firing EncounterCompletedEvent also reaches the production EncounterCompletedInvoiceListener
    // (issue 038), which would otherwise try to persist an invoice against the event's encounterId
    // and fail with a FK violation since this test's event doesn't reference a real, persisted
    // encounter. Mocked out here since this test only cares about CDI event delivery.
    @InjectMock
    GenerateInvoiceUseCase generateInvoiceUseCase;

    @BeforeEach
    void clearObserver() {
        observer.getReceived().clear();
    }

    @Test
    void publish_is_observable_by_a_cdi_listener() {
        var event = new EncounterCompletedEvent(
                EncounterId.generate(), AppointmentId.generate(), PatientId.generate(), UserId.of("dr-smith"));

        publisher.publish(event);

        assertThat(observer.getReceived()).containsExactly(event);
    }
}
