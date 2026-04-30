package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import com.github.database.rider.cdi.api.DBRider;
import com.github.database.rider.core.api.dataset.DataSet;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import net.fmjaeschke.quantumhealth.application.ports.out.PatientRepository;
import net.fmjaeschke.quantumhealth.domain.model.Patient;
import net.fmjaeschke.quantumhealth.domain.model.PatientId;
import net.fmjaeschke.quantumhealth.domain.model.PatientQuery;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@DBRider
class JpaPatientRepositoryTest {

    @Inject
    PatientRepository repository;

    private static PatientQuery defaultQuery() {
        return new PatientQuery(Optional.empty(), Optional.empty(), 0, 20, PatientQuery.SortField.LAST_NAME, PatientQuery.SortDirection.ASC);
    }

    @Test
    @DataSet("datasets/patient.yml")
    @Transactional
    void findById_returns_seeded_patient() {
        var id = PatientId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        var found = repository.findById(id);

        assertThat(found).isPresent();
        assertThat(found.get()
                .getFirstName()).isEqualTo("Alice");
        assertThat(found.get()
                .getLastName()).isEqualTo("Smith");
        assertThat(found.get()
                .getDateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 15));
    }

    @Test
    @Transactional
    void save_persists_new_patient() {
        var patient = Patient.register("Bob", "Jones", LocalDate.of(1985, 3, 20));
        var savedId = repository.save(patient);

        assertThat(savedId).isEqualTo(patient.getId());
        assertThat(repository.findById(savedId)).isPresent();
    }

    @Test
    @DataSet("datasets/patient.yml")
    void findById_returns_empty_for_unknown_id() {
        var found = repository.findById(PatientId.generate());
        assertThat(found).isEmpty();
    }

    @Test
    @DataSet("datasets/patient.yml")
    void findAll_returns_seeded_patient() {
        var query = defaultQuery();
        var page = repository.findAll(query);

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.patients()).hasSize(1);
        assertThat(page.patients()
                .getFirst()
                .getLastName()).isEqualTo("Smith");
    }

    @Test
    @DataSet("datasets/patient.yml")
    void findAll_filters_by_search() {
        var query = new PatientQuery(Optional.of("smi"), Optional.empty(), 0, 20, PatientQuery.SortField.LAST_NAME, PatientQuery.SortDirection.ASC);
        var page = repository.findAll(query);

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.patients()
                .getFirst()
                .getLastName()).isEqualTo("Smith");
    }

    @Test
    @DataSet("datasets/patient.yml")
    void findAll_returns_empty_when_search_has_no_match() {
        var query = new PatientQuery(Optional.of("zzznomatch"), Optional.empty(), 0, 20, PatientQuery.SortField.LAST_NAME, PatientQuery.SortDirection.ASC);
        var page = repository.findAll(query);

        assertThat(page.totalElements()).isZero();
    }

    @Test
    @DataSet("datasets/patient.yml")
    void findAll_filters_by_dateOfBirth() {
        var query = new PatientQuery(Optional.empty(), Optional.of(LocalDate.of(1990, 5, 15)), 0, 20, PatientQuery.SortField.LAST_NAME, PatientQuery.SortDirection.ASC);
        var page = repository.findAll(query);

        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    @DataSet("datasets/empty.yml")
    void findByDoctor_returns_empty_page_when_no_appointments_exist() {
        var query = defaultQuery();
        var page = repository.findByDoctor(UserId.of("doctor-1"), query);

        assertThat(page.patients()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }
}
