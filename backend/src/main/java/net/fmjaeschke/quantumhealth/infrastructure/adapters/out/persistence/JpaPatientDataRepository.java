package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Repository;
import org.hibernate.query.Order;
import org.hibernate.query.restriction.Restriction;

import java.util.UUID;

@Repository
public interface JpaPatientDataRepository extends BasicRepository<JpaPatient, UUID> {

    @Insert
    JpaPatient insert(JpaPatient entity);

    @Find
    Page<JpaPatient> matching(Restriction<JpaPatient> restriction, Order<JpaPatient> order, PageRequest pageRequest);
}
