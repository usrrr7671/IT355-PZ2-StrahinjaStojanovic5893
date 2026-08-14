package rs.ac.metropolitan.it355.helpdesk.dto;

import jakarta.validation.constraints.Size;

/**
 * Izmena tiketa; sva polja su opciona, salje se samo ono sto se menja.
 *
 * Prioritet ovde sme da menja samo osoblje podrske - procena hitnosti je njihov
 * posao, inace bi svaki tiket vremenom postao "kriticno".
 */
public record UpdateTicketRequest(
        @Size(min = 5, max = 150, message = "Naslov mora imati izmedju 5 i 150 karaktera")
        String title,

        @Size(min = 10, max = 4000, message = "Opis mora imati izmedju 10 i 4000 karaktera")
        String description,

        Long categoryId,

        Long priorityId) {
}
