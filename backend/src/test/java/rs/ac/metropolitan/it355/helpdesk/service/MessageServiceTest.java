package rs.ac.metropolitan.it355.helpdesk.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import rs.ac.metropolitan.it355.helpdesk.dto.ConversationResponse;
import rs.ac.metropolitan.it355.helpdesk.dto.StartConversationRequest;
import rs.ac.metropolitan.it355.helpdesk.exception.BusinessRuleException;
import rs.ac.metropolitan.it355.helpdesk.model.Conversation;
import rs.ac.metropolitan.it355.helpdesk.model.Role;
import rs.ac.metropolitan.it355.helpdesk.model.User;
import rs.ac.metropolitan.it355.helpdesk.repository.ConversationRepository;
import rs.ac.metropolitan.it355.helpdesk.repository.MessageRepository;
import rs.ac.metropolitan.it355.helpdesk.repository.TicketRepository;
import rs.ac.metropolitan.it355.helpdesk.repository.UserRepository;
import rs.ac.metropolitan.it355.helpdesk.security.UserPrincipal;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Jedinicni testovi prepiske.
 *
 * Najvazniji test u klasi je onaj koji dokazuje da ni administrator ne moze da
 * otvori prepisku u kojoj ne ucestvuje - to je jedino mesto u aplikaciji gde
 * najvisa uloga nema nikakvu prednost.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MessageService - privatnost prepiske")
class MessageServiceTest {

    @Mock private ConversationRepository conversationRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private UserRepository userRepository;
    @Mock private TicketRepository ticketRepository;

    @InjectMocks private MessageService messageService;

    private User pera;
    private User mika;
    private User agent;
    private User admin;

    @BeforeEach
    void setUp() {
        pera = user(10L, "pera", Role.USER);
        mika = user(11L, "mika", Role.USER);
        agent = user(20L, "agent1", Role.AGENT);
        admin = user(30L, "admin", Role.ADMIN);
    }

    @Test
    @DisplayName("ni administrator ne moze da otvori tudju prepisku")
    void administratorNeVidiTudjuPrepisku() {
        Conversation prepiska = conversation(1L, pera, agent);
        given(conversationRepository.findByIdWithParticipants(1L)).willReturn(Optional.of(prepiska));

        assertThatThrownBy(() -> messageService.getById(1L, principal(admin)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Nemate pristup");

        // Poruke ne smeju ni da se procitaju, a kamoli da se obeleze kao procitane.
        verify(messageRepository, never()).markConversationAsRead(any(), any(), any());
    }

    @Test
    @DisplayName("ucesnik moze da otvori svoju prepisku")
    void ucesnikOtvaraPrepisku() {
        Conversation prepiska = conversation(1L, pera, agent);
        given(conversationRepository.findByIdWithParticipants(1L)).willReturn(Optional.of(prepiska));
        given(messageRepository.findByConversationWithSender(1L)).willReturn(List.of());

        ConversationResponse odgovor = messageService.getById(1L, principal(pera));

        assertThat(odgovor.counterpart().username()).isEqualTo("agent1");
        verify(messageRepository).markConversationAsRead(any(), any(), any());
    }

    @Test
    @DisplayName("korisnik ne sme da pise drugom korisniku")
    void korisnikNePiseKorisniku() {
        given(userRepository.findById(pera.getId())).willReturn(Optional.of(pera));
        given(userRepository.findById(mika.getId())).willReturn(Optional.of(mika));

        assertThatThrownBy(() -> messageService.start(
                new StartConversationRequest(mika.getId(), null, "Zdravo"), principal(pera)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("samo agentima");

        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("korisnik sme da pise agentu")
    void korisnikPiseAgentu() {
        given(userRepository.findById(pera.getId())).willReturn(Optional.of(pera));
        given(userRepository.findById(agent.getId())).willReturn(Optional.of(agent));
        given(conversationRepository.findBetween(pera.getId(), agent.getId())).willReturn(Optional.empty());
        given(conversationRepository.save(any(Conversation.class)))
                .willAnswer(poziv -> poziv.getArgument(0));
        given(messageRepository.save(any())).willAnswer(poziv -> poziv.getArgument(0));
        given(messageRepository.findByConversationWithSender(any())).willReturn(List.of());

        messageService.start(new StartConversationRequest(agent.getId(), null, "Dobar dan"), principal(pera));

        verify(conversationRepository).save(any(Conversation.class));
        verify(messageRepository).save(any());
    }

    @Test
    @DisplayName("ponovno zapocinjanje vraca postojecu prepisku umesto nove")
    void postojecaPrepiskaSeNeDuplira() {
        Conversation postojeca = conversation(5L, pera, agent);
        given(userRepository.findById(pera.getId())).willReturn(Optional.of(pera));
        given(userRepository.findById(agent.getId())).willReturn(Optional.of(agent));
        given(conversationRepository.findBetween(pera.getId(), agent.getId())).willReturn(Optional.of(postojeca));
        given(messageRepository.findByConversationWithSender(5L)).willReturn(List.of());

        ConversationResponse odgovor = messageService.start(
                new StartConversationRequest(agent.getId(), null, null), principal(pera));

        assertThat(odgovor.id()).isEqualTo(5L);
        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("prepiska sa samim sobom se odbija")
    void prepiskaSaSamimSobom() {
        assertThatThrownBy(() -> messageService.start(
                new StartConversationRequest(pera.getId(), null, "test"), principal(pera)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("samim sobom");
    }

    @Test
    @DisplayName("prepiska sa deaktiviranim nalogom se odbija")
    void deaktiviranSagovornik() {
        agent.setActive(false);
        given(userRepository.findById(pera.getId())).willReturn(Optional.of(pera));
        given(userRepository.findById(agent.getId())).willReturn(Optional.of(agent));

        assertThatThrownBy(() -> messageService.start(
                new StartConversationRequest(agent.getId(), null, "Dobar dan"), principal(pera)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("deaktiviran");
    }

    // ------------------------------------------------------------------

    private User user(Long id, String username, Role role) {
        User user = new User(username, username + "@test.rs", username.toUpperCase(), "hes", role);
        user.setId(id);
        return user;
    }

    private UserPrincipal principal(User user) {
        return new UserPrincipal(user.getId(), user.getUsername(), user.getPassword(),
                user.getFullName(), user.getRole(), user.isActive());
    }

    private Conversation conversation(Long id, User first, User second) {
        Conversation conversation = new Conversation(first, second, null);
        conversation.setId(id);
        return conversation;
    }
}
