package rs.ac.metropolitan.it355.helpdesk.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Zapocinjanje prepiske sa drugim korisnikom.
 *
 * @param recipientId sagovornik
 * @param ticketId    tiket povodom koga se prepiska vodi; opciono
 * @param content     prva poruka; opciona, prepiska moze da se otvori i prazna
 */
public record StartConversationRequest(
        @NotNull(message = "Primalac je obavezan")
        Long recipientId,

        Long ticketId,

        @Size(max = 2000, message = "Poruka sme imati najvise 2000 karaktera")
        String content) {
}
