package rs.ac.metropolitan.it355.helpdesk.dto;

import rs.ac.metropolitan.it355.helpdesk.model.Conversation;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Prepiska iz ugla jednog korisnika.
 *
 * Umesto oba ucesnika prikazuje se samo {@code counterpart} - sagovornik - jer
 * korisniku u listi razgovora nije zanimljivo sopstveno ime.
 *
 * @param messages popunjeno samo kod otvaranja jedne prepiske; u listi je null
 */
public record ConversationResponse(
        Long id,
        UserSummary counterpart,
        Long ticketId,
        String ticketTitle,
        String lastMessagePreview,
        long unreadCount,
        LocalDateTime lastMessageAt,
        LocalDateTime createdAt,
        List<MessageResponse> messages) {

    private static final int PREVIEW_LENGTH = 60;

    /** Red u listi prepiski - bez poruka, samo sa najavom poslednje. */
    public static ConversationResponse summary(Conversation conversation,
                                               Long viewerId,
                                               String lastMessage,
                                               long unreadCount) {
        return new ConversationResponse(
                conversation.getId(),
                UserSummary.from(conversation.counterpartOf(viewerId)),
                conversation.getTicket() != null ? conversation.getTicket().getId() : null,
                conversation.getTicket() != null ? conversation.getTicket().getTitle() : null,
                preview(lastMessage),
                unreadCount,
                conversation.getLastMessageAt(),
                conversation.getCreatedAt(),
                null);
    }

    /** Otvorena prepiska sa svim porukama. */
    public static ConversationResponse withMessages(Conversation conversation,
                                                    Long viewerId,
                                                    List<MessageResponse> messages) {
        String lastMessage = messages.isEmpty() ? null : messages.get(messages.size() - 1).content();
        return new ConversationResponse(
                conversation.getId(),
                UserSummary.from(conversation.counterpartOf(viewerId)),
                conversation.getTicket() != null ? conversation.getTicket().getId() : null,
                conversation.getTicket() != null ? conversation.getTicket().getTitle() : null,
                preview(lastMessage),
                0,
                conversation.getLastMessageAt(),
                conversation.getCreatedAt(),
                messages);
    }

    private static String preview(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= PREVIEW_LENGTH ? text : text.substring(0, PREVIEW_LENGTH) + "...";
    }
}
