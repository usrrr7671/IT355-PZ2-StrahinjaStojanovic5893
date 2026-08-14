package rs.ac.metropolitan.it355.helpdesk.dto;

import rs.ac.metropolitan.it355.helpdesk.model.TicketStatus;

import java.util.Map;

/**
 * Zbirni pokazatelji za nadzornu tablu podrske.
 *
 * @param ticketsByStatus   broj tiketa po statusu
 * @param ticketsByCategory broj tiketa po kategoriji, od najopterecenije
 * @param slaBreached       broj otvorenih tiketa kojima je istekao SLA rok
 * @param assignedToMe      broj tiketa u radnom redu prijavljenog agenta
 */
public record DashboardStatsResponse(
        long totalTickets,
        long unassignedTickets,
        long slaBreached,
        long assignedToMe,
        Map<TicketStatus, Long> ticketsByStatus,
        Map<String, Long> ticketsByCategory) {
}
