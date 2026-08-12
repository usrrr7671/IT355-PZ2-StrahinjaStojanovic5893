package rs.ac.metropolitan.it355.helpdesk.model;

import java.util.Set;

/**
 * Zivotni ciklus tiketa. Prelazi izmedju statusa nisu proizvoljni - svaki status
 * zna u koje statuse sme da predje, cime se poslovna pravila drze na jednom mestu.
 */
public enum TicketStatus {

    /** Tiket je upravo kreiran i jos uvek nije dodeljen agentu. */
    NEW,

    /** Agent je preuzeo tiket, ali rad jos nije zapocet. */
    OPEN,

    /** Agent aktivno radi na tiketu. */
    IN_PROGRESS,

    /** Agent smatra da je problem resen i ceka se potvrda korisnika. */
    RESOLVED,

    /** Tiket je zatvoren. */
    CLOSED,

    /** Korisnik nije zadovoljan resenjem i ponovo je otvorio tiket. */
    REOPENED;

    public Set<TicketStatus> allowedTransitions() {
        return switch (this) {
            case NEW -> Set.of(OPEN, IN_PROGRESS, CLOSED);
            case OPEN -> Set.of(IN_PROGRESS, RESOLVED, CLOSED);
            case IN_PROGRESS -> Set.of(RESOLVED, OPEN, CLOSED);
            case RESOLVED -> Set.of(CLOSED, REOPENED);
            case REOPENED -> Set.of(IN_PROGRESS, RESOLVED, CLOSED);
            case CLOSED -> Set.of(REOPENED);
        };
    }

    public boolean canTransitionTo(TicketStatus target) {
        return allowedTransitions().contains(target);
    }

    /** Statusi u kojima se tiket smatra zavrsenim. */
    public boolean isTerminal() {
        return this == CLOSED;
    }
}
