package rs.ac.metropolitan.it355.helpdesk.dto;

import rs.ac.metropolitan.it355.helpdesk.model.TicketStatus;
import rs.ac.metropolitan.it355.helpdesk.model.TicketStatusHistory;

import java.time.LocalDateTime;

public record TicketHistoryResponse(
        Long id,
        UserSummary changedBy,
        TicketStatus oldStatus,
        TicketStatus newStatus,
        String note,
        LocalDateTime changedAt) {

    public static TicketHistoryResponse from(TicketStatusHistory entry) {
        return new TicketHistoryResponse(
                entry.getId(),
                UserSummary.from(entry.getChangedBy()),
                entry.getOldStatus(),
                entry.getNewStatus(),
                entry.getNote(),
                entry.getChangedAt());
    }
}
