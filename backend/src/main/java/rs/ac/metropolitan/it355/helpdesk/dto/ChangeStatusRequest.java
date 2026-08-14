package rs.ac.metropolitan.it355.helpdesk.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import rs.ac.metropolitan.it355.helpdesk.model.TicketStatus;

public record ChangeStatusRequest(
        @NotNull(message = "Novi status je obavezan")
        TicketStatus status,

        /** Obrazlozenje promene; upisuje se u istoriju tiketa. */
        @Size(max = 500, message = "Napomena sme imati najvise 500 karaktera")
        String note) {
}
