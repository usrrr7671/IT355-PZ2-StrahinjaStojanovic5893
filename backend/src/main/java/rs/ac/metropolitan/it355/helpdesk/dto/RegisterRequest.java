package rs.ac.metropolitan.it355.helpdesk.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Javna registracija uvek kreira nalog sa ulogom USER.
 * Uloga se namerno ne prima od klijenta - u suprotnom bi svako mogao
 * da se registruje kao administrator. Naloge agenata kreira administrator.
 */
public record RegisterRequest(
        @NotBlank(message = "Korisnicko ime je obavezno")
        @Size(min = 3, max = 50, message = "Korisnicko ime mora imati izmedju 3 i 50 karaktera")
        String username,

        @NotBlank(message = "E-adresa je obavezna")
        @Email(message = "E-adresa nije u ispravnom formatu")
        @Size(max = 120)
        String email,

        @NotBlank(message = "Ime i prezime su obavezni")
        @Size(max = 120)
        String fullName,

        @NotBlank(message = "Lozinka je obavezna")
        @Size(min = 6, max = 72, message = "Lozinka mora imati izmedju 6 i 72 karaktera")
        String password) {
}
