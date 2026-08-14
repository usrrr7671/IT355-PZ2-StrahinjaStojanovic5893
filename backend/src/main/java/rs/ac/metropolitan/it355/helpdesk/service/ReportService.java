package rs.ac.metropolitan.it355.helpdesk.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.metropolitan.it355.helpdesk.dto.DashboardStatsResponse;
import rs.ac.metropolitan.it355.helpdesk.model.Ticket;
import rs.ac.metropolitan.it355.helpdesk.model.TicketStatus;
import rs.ac.metropolitan.it355.helpdesk.repository.TicketRepository;
import rs.ac.metropolitan.it355.helpdesk.security.UserPrincipal;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Zbirni izvestaji za osoblje podrske.
 *
 * Brojanje se radi upitima sa GROUP BY, a ne ucitavanjem svih tiketa u memoriju -
 * na vecoj bazi je razlika izmedju jednog agregatnog upita i ucitavanja desetina
 * hiljada redova presudna.
 */
@Service
public class ReportService {

    private final TicketRepository ticketRepository;

    public ReportService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse dashboard(UserPrincipal principal) {
        Map<TicketStatus, Long> byStatus = new EnumMap<>(TicketStatus.class);
        for (TicketStatus status : TicketStatus.values()) {
            byStatus.put(status, 0L);   // statusi bez ijednog tiketa treba da se vide kao nula
        }
        for (Object[] row : ticketRepository.countGroupedByStatus()) {
            byStatus.put((TicketStatus) row[0], (Long) row[1]);
        }

        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (Object[] row : ticketRepository.countGroupedByCategory()) {
            byCategory.put((String) row[0], (Long) row[1]);
        }

        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long assignedToMe = ticketRepository.countByAssigneeIdAndStatusNot(
                principal.getId(), TicketStatus.CLOSED);

        return new DashboardStatsResponse(
                total,
                countUnassigned(),
                countSlaBreached(),
                assignedToMe,
                byStatus,
                byCategory);
    }

    private long countUnassigned() {
        return ticketRepository.countByStatus(TicketStatus.NEW);
    }

    /**
     * Prekoracenje SLA roka se ne moze izraziti prostim upitom jer rok nije kolona,
     * vec se racuna kao vreme kreiranja uvecano za SLA sate prioriteta.
     *
     * Zato se iz baze dovlace samo tiketi koji jos nisu zatvoreni - zatvoreni,
     * kojih vremenom bude najvise, uopste ne ulaze u memoriju.
     */
    private long countSlaBreached() {
        LocalDateTime now = LocalDateTime.now();
        List<Ticket> active = ticketRepository.findActiveWithPriority(TicketStatus.CLOSED);

        return active.stream()
                .filter(ticket -> {
                    LocalDateTime deadline = ticket.getSlaDeadline();
                    return deadline != null && now.isAfter(deadline);
                })
                .count();
    }
}
