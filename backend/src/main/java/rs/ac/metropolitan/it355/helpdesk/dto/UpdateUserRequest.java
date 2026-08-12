package rs.ac.metropolitan.it355.helpdesk.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import rs.ac.metropolitan.it355.helpdesk.model.Role;

/**
 * Izmena naloga iz administratorskog panela.
 * Sva polja su opciona - salje se samo ono sto se menja.
 */
public record UpdateUserRequest(
        @Email(message = "E-adresa nije u ispravnom formatu")
        String email,

        @Size(max = 120)
        String fullName,

        Role role,

        Boolean active) {
}
