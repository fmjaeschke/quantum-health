package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.keycloak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import net.fmjaeschke.quantumhealth.application.ports.out.DoctorPort;
import net.fmjaeschke.quantumhealth.domain.model.Doctor;
import net.fmjaeschke.quantumhealth.domain.model.UserId;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.admin.client.Keycloak;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class KeycloakDoctorAdapter implements DoctorPort {

    private final Keycloak keycloak;
    private final String realm;

    public KeycloakDoctorAdapter(Keycloak keycloak,
                                  @ConfigProperty(name = "quantum-health.keycloak.realm",
                                                  defaultValue = "quantum-health") String realm) {
        this.keycloak = keycloak;
        this.realm = realm;
    }

    @Override
    public List<Doctor> findByRole(String role) {
        return keycloak.realm(realm).roles().get(role).getUserMembers()
                .stream()
                .map(u -> new Doctor(UserId.of(u.getId()), u.getFirstName() + " " + u.getLastName()))
                .toList();
    }

    @Override
    public Optional<Doctor> findById(UserId id) {
        try {
            var u = keycloak.realm(realm).users().get(id.value()).toRepresentation();
            if (u == null) return Optional.empty();
            return Optional.of(new Doctor(UserId.of(u.getId()), u.getFirstName() + " " + u.getLastName()));
        } catch (NotFoundException _) {
            return Optional.empty();
        }
    }
}
