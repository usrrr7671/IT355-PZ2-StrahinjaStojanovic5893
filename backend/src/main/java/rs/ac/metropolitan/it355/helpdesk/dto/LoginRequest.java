package rs.ac.metropolitan.it355.helpdesk.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Korisnicko ime je obavezno")
        String username,

        @NotBlank(message = "Lozinka je obavezna")
        String password) {
}
