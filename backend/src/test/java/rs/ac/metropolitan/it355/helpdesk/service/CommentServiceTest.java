package rs.ac.metropolitan.it355.helpdesk.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import rs.ac.metropolitan.it355.helpdesk.dto.CommentResponse;
import rs.ac.metropolitan.it355.helpdesk.dto.CreateCommentRequest;
import rs.ac.metropolitan.it355.helpdesk.model.Category;
import rs.ac.metropolitan.it355.helpdesk.model.Comment;
import rs.ac.metropolitan.it355.helpdesk.model.Priority;
import rs.ac.metropolitan.it355.helpdesk.model.Role;
import rs.ac.metropolitan.it355.helpdesk.model.Ticket;
import rs.ac.metropolitan.it355.helpdesk.model.TicketStatus;
import rs.ac.metropolitan.it355.helpdesk.model.User;
import rs.ac.metropolitan.it355.helpdesk.repository.CommentRepository;
import rs.ac.metropolitan.it355.helpdesk.security.UserPrincipal;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Jedinicni testovi komentara, sa tezistem na internim beleskama podrske.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService - interne beleske i javni komentari")
class CommentServiceTest {

    @Mock private CommentRepository commentRepository;
    @Mock private TicketService ticketService;
    @Mock private UserService userService;

    @InjectMocks private CommentService commentService;

    @Captor private ArgumentCaptor<Comment> commentCaptor;

    private User pera;
    private User agent;
    private Ticket tiket;

    @BeforeEach
    void setUp() {
        pera = user(10L, "pera", Role.USER);
        agent = user(20L, "agent1", Role.AGENT);

        Category kategorija = new Category("Softver", "Programi");
        kategorija.setId(1L);
        Priority prioritet = new Priority("Srednji", 2, 24);
        prioritet.setId(1L);

        tiket = new Ticket("Naslov", "Opis", pera, kategorija, prioritet);
        tiket.setId(1L);
        tiket.setStatus(TicketStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("agent moze da napise internu belesku")
    void agentPiseInternuBelesku() {
        given(ticketService.findTicketOrThrow(1L)).willReturn(tiket);
        given(userService.findEntityById(agent.getId())).willReturn(agent);
        given(commentRepository.save(any(Comment.class))).willAnswer(poziv -> poziv.getArgument(0));

        CommentResponse odgovor = commentService.addComment(
                1L, new CreateCommentRequest("Korisnik je vec triput zvao", true), principal(agent));

        assertThat(odgovor.internal()).isTrue();
        verify(commentRepository).save(commentCaptor.capture());
        assertThat(commentCaptor.getValue().isInternal()).isTrue();
    }

    @Test
    @DisplayName("korisnik ne moze da napise internu belesku")
    void korisnikNeMozeInternuBelesku() {
        given(ticketService.findTicketOrThrow(1L)).willReturn(tiket);

        assertThatThrownBy(() -> commentService.addComment(
                1L, new CreateCommentRequest("Pokusaj skrivanja", true), principal(pera)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Interne beleske");

        // Zahtev se odbija, a ne pretvara tiho u javni komentar.
        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("korisnik moze da napise javni komentar na svom tiketu")
    void korisnikPiseJavniKomentar() {
        given(ticketService.findTicketOrThrow(1L)).willReturn(tiket);
        given(userService.findEntityById(pera.getId())).willReturn(pera);
        given(commentRepository.save(any(Comment.class))).willAnswer(poziv -> poziv.getArgument(0));

        CommentResponse odgovor = commentService.addComment(
                1L, new CreateCommentRequest("Hvala na brzoj reakciji", null), principal(pera));

        assertThat(odgovor.internal()).isFalse();
    }

    @Test
    @DisplayName("korisniku se interne beleske ne dovlace iz baze")
    void korisnikCitaSamoJavneKomentare() {
        given(ticketService.findTicketOrThrow(1L)).willReturn(tiket);
        given(commentRepository.findByTicketIdAndInternalFalseOrderByCreatedAtAsc(1L)).willReturn(List.of());

        commentService.listForTicket(1L, principal(pera));

        verify(commentRepository).findByTicketIdAndInternalFalseOrderByCreatedAtAsc(1L);
        verify(commentRepository, never()).findByTicketIdOrderByCreatedAtAsc(anyLong());
    }

    @Test
    @DisplayName("agentu se dovlace svi komentari, ukljucujuci interne")
    void agentCitaSveKomentare() {
        given(ticketService.findTicketOrThrow(1L)).willReturn(tiket);
        given(commentRepository.findByTicketIdOrderByCreatedAtAsc(1L)).willReturn(List.of());

        commentService.listForTicket(1L, principal(agent));

        verify(commentRepository).findByTicketIdOrderByCreatedAtAsc(1L);
        verify(commentRepository, never()).findByTicketIdAndInternalFalseOrderByCreatedAtAsc(anyLong());
    }

    // ------------------------------------------------------------------

    private User user(Long id, String username, Role role) {
        User user = new User(username, username + "@test.rs", username.toUpperCase(), "hes", role);
        user.setId(id);
        return user;
    }

    private UserPrincipal principal(User user) {
        return new UserPrincipal(user.getId(), user.getUsername(), user.getPassword(),
                user.getFullName(), user.getRole(), true);
    }
}
