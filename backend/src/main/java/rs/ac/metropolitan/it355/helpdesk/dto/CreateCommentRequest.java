package rs.ac.metropolitan.it355.helpdesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param internal zahtev za internu belesku. Postavljanje na true je samo zelja
 *                 klijenta - servis ga odbija ako posiljalac nije agent ili
 *                 administrator, jer bi inace korisnik mogao da napise komentar
 *                 nevidljiv samom sebi.
 */
public record CreateCommentRequest(
        @NotBlank(message = "Sadrzaj komentara je obavezan")
        @Size(max = 2000, message = "Komentar sme imati najvise 2000 karaktera")
        String content,

        Boolean internal) {
}
