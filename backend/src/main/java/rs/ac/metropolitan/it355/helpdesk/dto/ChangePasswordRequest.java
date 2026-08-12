package rs.ac.metropolitan.it355.helpdesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Trenutna lozinka je obavezna")
        String currentPassword,

        @NotBlank(message = "Nova lozinka je obavezna")
        @Size(min = 6, max = 72, message = "Nova lozinka mora imati izmedju 6 i 72 karaktera")
        String newPassword) {
}
