package rs.ac.metropolitan.it355.helpdesk.dto;

import rs.ac.metropolitan.it355.helpdesk.model.Ticket;
import rs.ac.metropolitan.it355.helpdesk.model.TicketStatus;

import java.time.LocalDateTime;

/**
 * Red u listi tiketa. Namerno ne sadrzi opis, komentare ni istoriju - lista od
 * pedeset tiketa ne treba da povuce i sve njihove komentare iz baze.
 */
public record TicketSummaryResponse(
        Long id,
        String title,
        TicketStatus status,
        String categoryName,
        String priorityName,
        int priorityLevel,
        UserSummary reporter,
        UserSummary assignee,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime slaDeadline,
        boolean slaBreached) {

    public static TicketSummaryResponse from(Ticket ticket) {
        LocalDateTime deadline = ticket.getSlaDeadline();
        boolean breached = deadline != null
                && !ticket.getStatus().isTerminal()
                && LocalDateTime.now().isAfter(deadline);

        return new TicketSummaryResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getStatus(),
                ticket.getCategory().getName(),
                ticket.getPriority().getName(),
                ticket.getPriority().getLevel(),
                UserSummary.from(ticket.getReporter()),
                UserSummary.from(ticket.getAssignee()),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                deadline,
                breached);
    }
}
