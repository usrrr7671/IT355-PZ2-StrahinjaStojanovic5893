package rs.ac.metropolitan.it355.helpdesk.dto;

/**
 * Dodela tiketa agentu.
 *
 * @param agentId id agenta; kada je null, tiket se dodeljuje agentu koji trenutno
 *                ima najmanje otvorenih tiketa (automatska raspodela)
 */
public record AssignTicketRequest(Long agentId) {
}
