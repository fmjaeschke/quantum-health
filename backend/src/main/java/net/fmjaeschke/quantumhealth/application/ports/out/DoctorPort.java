package net.fmjaeschke.quantumhealth.application.ports.out;

import net.fmjaeschke.quantumhealth.domain.model.Doctor;
import net.fmjaeschke.quantumhealth.domain.model.UserId;

import java.util.List;
import java.util.Optional;

public interface DoctorPort {
    List<Doctor> findByRole(String role);
    Optional<Doctor> findById(UserId id);
}
