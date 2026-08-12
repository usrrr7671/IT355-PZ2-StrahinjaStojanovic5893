package rs.ac.metropolitan.it355.helpdesk.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import rs.ac.metropolitan.it355.helpdesk.model.Role;

/**
 * Kreiranje naloga iz administratorskog panela. Za razliku od javne registracije,
 * ovde se uloga zadaje - tako nastaju nalozi agenata i drugih administratora.
 */
public record CreateUserRequest(
        @NotBlank(message = "Korisnicko ime je obavezno")
        @Size(min = 3, max = 50)
        String username,

        @NotBlank(message = "E-adresa je obavezna")
        @Email(message = "E-adresa nije u ispravnom formatu")
        String email,

        @NotBlank(message = "Ime i prezime su obavezni")
        String fullName,

        @NotBlank(message = "Lozinka je obavezna")
        @Size(min = 6, max = 72)
        String password,

        @NotNull(message = "Uloga je obavezna")
        Role role) {
}
