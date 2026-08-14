package rs.ac.metropolitan.it355.helpdesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Prijava novog tiketa.
 *
 * Prijavilac se ne prima iz zahteva, vec se cita iz tokena - inace bi korisnik
 * mogao da prijavi tiket u tudje ime. Isto vazi i za status i zaduzenog agenta:
 * novi tiket uvek krece kao NEW i nedodeljen.
 */
public record CreateTicketRequest(
        @NotBlank(message = "Naslov tiketa je obavezan")
        @Size(min = 5, max = 150, message = "Naslov mora imati izmedju 5 i 150 karaktera")
        String title,

        @NotBlank(message = "Opis problema je obavezan")
        @Size(min = 10, max = 4000, message = "Opis mora imati izmedju 10 i 4000 karaktera")
        String description,

        @NotNull(message = "Kategorija je obavezna")
        Long categoryId,

        @NotNull(message = "Prioritet je obavezan")
        Long priorityId) {
}
