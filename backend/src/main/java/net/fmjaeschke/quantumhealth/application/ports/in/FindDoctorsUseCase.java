package net.fmjaeschke.quantumhealth.application.ports.in;

import net.fmjaeschke.quantumhealth.domain.model.Doctor;

import java.util.List;

public interface FindDoctorsUseCase {
    List<Doctor> findDoctors();
}
