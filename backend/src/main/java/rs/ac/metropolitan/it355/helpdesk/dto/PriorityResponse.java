package rs.ac.metropolitan.it355.helpdesk.dto;

import rs.ac.metropolitan.it355.helpdesk.model.Priority;

public record PriorityResponse(Long id, String name, int level, int slaHours) {

    public static PriorityResponse from(Priority priority) {
        return new PriorityResponse(
                priority.getId(),
                priority.getName(),
                priority.getLevel(),
                priority.getSlaHours());
    }
}
