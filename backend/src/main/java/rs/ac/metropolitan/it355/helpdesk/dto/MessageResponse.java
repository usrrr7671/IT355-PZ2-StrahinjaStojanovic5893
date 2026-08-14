package rs.ac.metropolitan.it355.helpdesk.dto;

import rs.ac.metropolitan.it355.helpdesk.model.Message;

import java.time.LocalDateTime;

/**
 * @param mine da li je poruku poslao korisnik koji je zatrazio prepisku;
 *             frontend na osnovu toga bira stranu na kojoj crta oblacic
 */
public record MessageResponse(
        Long id,
        Long conversationId,
        UserSummary sender,
        String content,
        boolean mine,
        LocalDateTime sentAt,
        LocalDateTime readAt) {

    public static MessageResponse from(Message message, Long viewerId) {
        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                UserSummary.from(message.getSender()),
                message.getContent(),
                message.getSender().getId().equals(viewerId),
                message.getSentAt(),
                message.getReadAt());
    }
}
