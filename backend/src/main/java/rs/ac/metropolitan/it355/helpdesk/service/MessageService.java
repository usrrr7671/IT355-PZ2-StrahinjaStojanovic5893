package rs.ac.metropolitan.it355.helpdesk.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.metropolitan.it355.helpdesk.dto.ConversationResponse;
import rs.ac.metropolitan.it355.helpdesk.dto.MessageResponse;
import rs.ac.metropolitan.it355.helpdesk.dto.SendMessageRequest;
import rs.ac.metropolitan.it355.helpdesk.dto.StartConversationRequest;
import rs.ac.metropolitan.it355.helpdesk.exception.BusinessRuleException;
import rs.ac.metropolitan.it355.helpdesk.exception.ResourceNotFoundException;
import rs.ac.metropolitan.it355.helpdesk.model.Conversation;
import rs.ac.metropolitan.it355.helpdesk.model.Message;
import rs.ac.metropolitan.it355.helpdesk.model.Ticket;
import rs.ac.metropolitan.it355.helpdesk.model.User;
import rs.ac.metropolitan.it355.helpdesk.repository.ConversationRepository;
import rs.ac.metropolitan.it355.helpdesk.repository.MessageRepository;
import rs.ac.metropolitan.it355.helpdesk.repository.TicketRepository;
import rs.ac.metropolitan.it355.helpdesk.repository.UserRepository;
import rs.ac.metropolitan.it355.helpdesk.security.UserPrincipal;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Privatna prepiska izmedju dva korisnika - posebna funkcionalnost projekta.
 *
 * Razlika u odnosu na komentare je sustinska: komentar pripada tiketu i vidi ga
 * svako ko ima pristup tom tiketu, dok je prepiska kanal izmedju tacno dve osobe.
 * Zato ovde vazi strozije pravilo: <b>ni administrator ne moze da cita tudju prepisku</b>.
 * Jedini kriterijum pristupa je ucesce u razgovoru, ne uloga.
 */
@Service
public class MessageService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    public MessageService(ConversationRepository conversationRepository,
                          MessageRepository messageRepository,
                          UserRepository userRepository,
                          TicketRepository ticketRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
    }

    /** Sve prepiske prijavljenog korisnika, najsvezija prva. */
    @Transactional(readOnly = true)
    public List<ConversationResponse> listMine(UserPrincipal principal) {
        Long viewerId = principal.getId();

        return conversationRepository.findAllForUser(viewerId).stream()
                .map(conversation -> {
                    List<Message> messages =
                            messageRepository.findByConversationIdOrderBySentAtAsc(conversation.getId());
                    String lastMessage = messages.isEmpty()
                            ? null
                            : messages.get(messages.size() - 1).getContent();
                    long unread = messageRepository.countUnreadInConversation(conversation.getId(), viewerId);

                    return ConversationResponse.summary(conversation, viewerId, lastMessage, unread);
                })
                .toList();
    }

    /**
     * Otvara prepisku i uz to obelezava tudje poruke kao procitane.
     *
     * Metoda nije samo za citanje, pa transakcija nije readOnly - otvaranje razgovora
     * po prirodi menja stanje neprocitanih poruka.
     */
    @Transactional
    public ConversationResponse getById(Long conversationId, UserPrincipal principal) {
        // Ucesnici i tiket se ucitavaju odmah (JOIN FETCH), jer masovni UPDATE ispod
        // cisti persistence context - lazy proxy bi posle toga bio neupotrebljiv.
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Prepiska", conversationId));
        assertParticipant(conversation, principal);

        messageRepository.markConversationAsRead(conversationId, principal.getId(), LocalDateTime.now());

        List<MessageResponse> messages =
                messageRepository.findByConversationWithSender(conversationId).stream()
                        .map(message -> MessageResponse.from(message, principal.getId()))
                        .toList();

        return ConversationResponse.withMessages(conversation, principal.getId(), messages);
    }

    /**
     * Zapocinje prepisku, ili vraca postojecu ako je par vec razgovarao - dva
     * paralelna razgovora izmedju istih osoba nemaju smisla.
     */
    @Transactional
    public ConversationResponse start(StartConversationRequest request, UserPrincipal principal) {
        if (request.recipientId().equals(principal.getId())) {
            throw new BusinessRuleException("Ne mozete zapoceti prepisku sa samim sobom");
        }

        User sender = findUserOrThrow(principal.getId());
        User recipient = findUserOrThrow(request.recipientId());

        if (!recipient.isActive()) {
            throw new BusinessRuleException("Nalog '" + recipient.getUsername() + "' je deaktiviran");
        }
        assertMayContact(sender, recipient);

        Conversation conversation = conversationRepository
                .findBetween(sender.getId(), recipient.getId())
                .orElseGet(() -> {
                    Ticket ticket = request.ticketId() == null ? null
                            : ticketRepository.findById(request.ticketId())
                                    .orElseThrow(() -> new ResourceNotFoundException("Tiket", request.ticketId()));
                    return conversationRepository.save(new Conversation(sender, recipient, ticket));
                });

        if (request.content() != null && !request.content().isBlank()) {
            persistMessage(conversation, sender, request.content());
        }

        List<MessageResponse> messages =
                messageRepository.findByConversationWithSender(conversation.getId()).stream()
                        .map(message -> MessageResponse.from(message, principal.getId()))
                        .toList();

        return ConversationResponse.withMessages(conversation, principal.getId(), messages);
    }

    @Transactional
    public MessageResponse sendMessage(Long conversationId, SendMessageRequest request, UserPrincipal principal) {
        Conversation conversation = findOrThrow(conversationId);
        assertParticipant(conversation, principal);

        User counterpart = conversation.counterpartOf(principal.getId());
        if (!counterpart.isActive()) {
            throw new BusinessRuleException("Sagovornik vise nema aktivan nalog");
        }

        User sender = findUserOrThrow(principal.getId());
        Message message = persistMessage(conversation, sender, request.content());

        return MessageResponse.from(message, principal.getId());
    }

    /** Ukupan broj neprocitanih poruka - prikazuje se kao znacka u meniju. */
    @Transactional(readOnly = true)
    public long unreadCount(UserPrincipal principal) {
        return messageRepository.countUnreadForUser(principal.getId());
    }

    // ==================================================================
    // Pravila pristupa
    // ==================================================================

    /**
     * Pristup prepisci ima iskljucivo njen ucesnik.
     *
     * Uloga se ovde namerno ne gleda: administrator ima najsira ovlascenja nad
     * tiketima i nalozima, ali privatna prepiska dva korisnika nije podatak
     * sistema - ona pripada sagovornicima.
     */
    private void assertParticipant(Conversation conversation, UserPrincipal principal) {
        if (!conversation.hasParticipant(principal.getId())) {
            throw new AccessDeniedException("Nemate pristup ovoj prepisci");
        }
    }

    /**
     * Krajnji korisnik sme da pise samo osoblju podrske; osoblje sme svima.
     *
     * Bez ovog pravila help desk bi postao opsti dopisnik - korisnik bi mogao da
     * nasumicnim pogadjanjem id-jeva pise bilo kome, ukljucujuci i osobe sa kojima
     * nema nikakve veze.
     */
    private void assertMayContact(User sender, User recipient) {
        if (!sender.isStaff() && !recipient.isStaff()) {
            throw new AccessDeniedException("Poruke mozete slati samo agentima podrske");
        }
    }

    // ==================================================================
    // Pomocne metode
    // ==================================================================

    private Message persistMessage(Conversation conversation, User sender, String content) {
        Message message = new Message(conversation, sender, content);
        conversation.addMessage(message);
        return messageRepository.save(message);
    }

    private Conversation findOrThrow(Long id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prepiska", id));
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Korisnik", id));
    }
}
