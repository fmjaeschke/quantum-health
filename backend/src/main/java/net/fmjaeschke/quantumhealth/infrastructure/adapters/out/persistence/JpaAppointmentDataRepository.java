package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import org.hibernate.query.restriction.Restriction;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaAppointmentDataRepository extends BasicRepository<JpaAppointment, UUID> {

    @Insert
    JpaAppointment insert(JpaAppointment entity);

    @Find
    List<JpaAppointment> findByDoctorId(String doctorId);

    @Find
    Page<JpaAppointment> matching(Restriction<JpaAppointment> restriction, PageRequest pageRequest);

    @Query("select count(a) > 0 from JpaAppointment a where a.doctorId = :doctorId and a.patientId = :patientId")
    boolean existsByDoctorIdAndPatientId(String doctorId, UUID patientId);
}
