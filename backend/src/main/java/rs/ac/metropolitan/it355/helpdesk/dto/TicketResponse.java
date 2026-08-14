package rs.ac.metropolitan.it355.helpdesk.dto;

import rs.ac.metropolitan.it355.helpdesk.model.Ticket;
import rs.ac.metropolitan.it355.helpdesk.model.TicketStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Pun prikaz jednog tiketa.
 *
 * Polje {@code allowedTransitions} govori frontendu koja dugmad za promenu statusa
 * da prikaze; time pravila zivotnog ciklusa ostaju iskljucivo na serveru, a interfejs
 * ne mora da ih ponavlja i da se razidje sa njima.
 */
public record TicketResponse(
        Long id,
        String title,
        String description,
        TicketStatus status,
        CategoryResponse category,
        PriorityResponse priority,
        UserSummary reporter,
        UserSummary assignee,
        List<CommentResponse> comments,
        List<TicketHistoryResponse> history,
        Set<TicketStatus> allowedTransitions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime closedAt,
        LocalDateTime slaDeadline,
        boolean slaBreached) {

    public static TicketResponse from(Ticket ticket,
                                      List<CommentResponse> visibleComments,
                                      List<TicketHistoryResponse> history) {
        LocalDateTime deadline = ticket.getSlaDeadline();
        boolean breached = deadline != null
                && !ticket.getStatus().isTerminal()
                && LocalDateTime.now().isAfter(deadline);

        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                CategoryResponse.from(ticket.getCategory()),
                PriorityResponse.from(ticket.getPriority()),
                UserSummary.from(ticket.getReporter()),
                UserSummary.from(ticket.getAssignee()),
                visibleComments,
                history,
                ticket.getStatus().allowedTransitions(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getClosedAt(),
                deadline,
                breached);
    }
}
