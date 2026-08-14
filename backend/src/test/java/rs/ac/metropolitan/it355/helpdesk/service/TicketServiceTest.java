package rs.ac.metropolitan.it355.helpdesk.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import rs.ac.metropolitan.it355.helpdesk.dto.AssignTicketRequest;
import rs.ac.metropolitan.it355.helpdesk.dto.ChangeStatusRequest;
import rs.ac.metropolitan.it355.helpdesk.dto.CreateTicketRequest;
import rs.ac.metropolitan.it355.helpdesk.dto.TicketResponse;
import rs.ac.metropolitan.it355.helpdesk.exception.BusinessRuleException;
import rs.ac.metropolitan.it355.helpdesk.exception.ResourceNotFoundException;
import rs.ac.metropolitan.it355.helpdesk.model.Category;
import rs.ac.metropolitan.it355.helpdesk.model.Priority;
import rs.ac.metropolitan.it355.helpdesk.model.Role;
import rs.ac.metropolitan.it355.helpdesk.model.Ticket;
import rs.ac.metropolitan.it355.helpdesk.model.TicketStatus;
import rs.ac.metropolitan.it355.helpdesk.model.User;
import rs.ac.metropolitan.it355.helpdesk.repository.CategoryRepository;
import rs.ac.metropolitan.it355.helpdesk.repository.CommentRepository;
import rs.ac.metropolitan.it355.helpdesk.repository.ConversationRepository;
import rs.ac.metropolitan.it355.helpdesk.repository.PriorityRepository;
import rs.ac.metropolitan.it355.helpdesk.repository.TicketRepository;
import rs.ac.metropolitan.it355.helpdesk.repository.TicketStatusHistoryRepository;
import rs.ac.metropolitan.it355.helpdesk.repository.UserRepository;
import rs.ac.metropolitan.it355.helpdesk.security.UserPrincipal;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Jedinicni testovi servisa tiketa.
 *
 * Servis se testira u izolaciji: svi repozitorijumi su Mockito duplikati, pa nema
 * ni baze ni Spring konteksta i test proverava iskljucivo poslovnu logiku.
 * Zato su ovi testovi trenutni - cela klasa se izvrsi za manje od sekunde.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TicketService - poslovna pravila i kontrola pristupa")
class TicketServiceTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private PriorityRepository priorityRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private TicketStatusHistoryRepository historyRepository;
    @Mock private ConversationRepository conversationRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserService userService;

    @InjectMocks private TicketService ticketService;

    @Captor private ArgumentCaptor<Long> reporterIdCaptor;

    private User pera;
    private User mika;
    private User agent;
    private Category kategorija;
    private Priority prioritet;

    private UserPrincipal peraPrincipal;
    private UserPrincipal mikaPrincipal;
    private UserPrincipal agentPrincipal;

    @BeforeEach
    void setUp() {
        pera = user(10L, "pera", "Petar Peric", Role.USER);
        mika = user(11L, "mika", "Mihajlo Mihajlovic", Role.USER);
        agent = user(20L, "agent1", "Marko Markovic", Role.AGENT);

        kategorija = new Category("Hardver", "Kvarovi opreme");
        kategorija.setId(1L);
        prioritet = new Priority("Visok", 3, 8);
        prioritet.setId(2L);

        peraPrincipal = principal(pera);
        mikaPrincipal = principal(mika);
        agentPrincipal = principal(agent);
    }

    @Nested
    @DisplayName("Vidljivost tiketa")
    class Vidljivost {

        @Test
        @DisplayName("prijavilac moze da otvori sopstveni tiket")
        void prijavilacVidiSvojTiket() {
            Ticket tiket = ticket(1L, pera, TicketStatus.NEW);
            given(ticketRepository.findByIdWithRelations(1L)).willReturn(Optional.of(tiket));
            given(commentRepository.findByTicketIdAndInternalFalseOrderByCreatedAtAsc(1L)).willReturn(List.of());
            given(historyRepository.findByTicketIdOrderByChangedAtAsc(1L)).willReturn(List.of());

            TicketResponse odgovor = ticketService.getById(1L, peraPrincipal);

            assertThat(odgovor.id()).isEqualTo(1L);
            assertThat(odgovor.reporter().username()).isEqualTo("pera");
        }

        @Test
        @DisplayName("korisnik ne moze da otvori tudji tiket")
        void korisnikNeVidiTudjiTiket() {
            Ticket tiket = ticket(1L, pera, TicketStatus.NEW);
            given(ticketRepository.findByIdWithRelations(1L)).willReturn(Optional.of(tiket));

            assertThatThrownBy(() -> ticketService.getById(1L, mikaPrincipal))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Nemate pristup");
        }

        @Test
        @DisplayName("agent moze da otvori tiket koji nije njegov")
        void agentVidiSvakiTiket() {
            Ticket tiket = ticket(1L, pera, TicketStatus.NEW);
            given(ticketRepository.findByIdWithRelations(1L)).willReturn(Optional.of(tiket));
            given(commentRepository.findByTicketIdOrderByCreatedAtAsc(1L)).willReturn(List.of());
            given(historyRepository.findByTicketIdOrderByChangedAtAsc(1L)).willReturn(List.of());

            TicketResponse odgovor = ticketService.getById(1L, agentPrincipal);

            assertThat(odgovor.id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("agentu se citaju svi komentari, korisniku samo javni")
        void interneBeleskeSeNeUcitavajuKorisniku() {
            Ticket tiket = ticket(1L, pera, TicketStatus.NEW);
            given(ticketRepository.findByIdWithRelations(1L)).willReturn(Optional.of(tiket));
            given(commentRepository.findByTicketIdAndInternalFalseOrderByCreatedAtAsc(1L)).willReturn(List.of());
            given(historyRepository.findByTicketIdOrderByChangedAtAsc(1L)).willReturn(List.of());

            ticketService.getById(1L, peraPrincipal);

            // Sustina pravila: za korisnika se poziva upit koji interne beleske
            // uopste ne dovlaci iz baze, umesto da se filtriraju posle ucitavanja.
            verify(commentRepository).findByTicketIdAndInternalFalseOrderByCreatedAtAsc(1L);
            verify(commentRepository, never()).findByTicketIdOrderByCreatedAtAsc(anyLong());
        }

        @Test
        @DisplayName("korisniku se filter po prijaviocu postavlja na serveru")
        void korisnikNeMozeDaIzlistaTudjeTikete() {
            Pageable strana = PageRequest.of(0, 20);
            Page<Ticket> prazna = new PageImpl<>(List.of());
            given(ticketRepository.search(any(), any(), any(), any(), any(), any(), eq(strana)))
                    .willReturn(prazna);

            // Korisnik pokusava da vidi tikete drugog korisnika (assigneeId 999).
            ticketService.search(null, null, null, 999L, null, strana, mikaPrincipal);

            verify(ticketRepository).search(any(), any(), any(), any(), reporterIdCaptor.capture(), any(), eq(strana));
            assertThat(reporterIdCaptor.getValue())
                    .as("prijavilac mora biti prisilno postavljen na samog korisnika")
                    .isEqualTo(mika.getId());
        }

        @Test
        @DisplayName("agentu se filter po prijaviocu ne postavlja")
        void agentPretrazujeSveTikete() {
            Pageable strana = PageRequest.of(0, 20);
            given(ticketRepository.search(any(), any(), any(), any(), any(), any(), eq(strana)))
                    .willReturn(new PageImpl<>(List.of()));

            ticketService.search(null, null, null, null, null, strana, agentPrincipal);

            verify(ticketRepository).search(any(), any(), any(), any(), reporterIdCaptor.capture(), any(), eq(strana));
            assertThat(reporterIdCaptor.getValue()).isNull();
        }
    }

    @Nested
    @DisplayName("Zivotni ciklus statusa")
    class Statusi {

        @Test
        @DisplayName("dozvoljen prelaz menja status i upisuje zapis u istoriju")
        void dozvoljenPrelaz() {
            Ticket tiket = ticket(1L, pera, TicketStatus.OPEN);
            given(ticketRepository.findByIdWithRelations(1L)).willReturn(Optional.of(tiket));
            given(userService.findEntityById(agent.getId())).willReturn(agent);
            given(commentRepository.findByTicketIdOrderByCreatedAtAsc(1L)).willReturn(List.of());

            TicketResponse odgovor = ticketService.changeStatus(
                    1L, new ChangeStatusRequest(TicketStatus.IN_PROGRESS, "Krecem na teren"), agentPrincipal);

            assertThat(odgovor.status()).isEqualTo(TicketStatus.IN_PROGRESS);
            assertThat(tiket.getHistory()).hasSize(1);
            assertThat(tiket.getHistory().get(0).getOldStatus()).isEqualTo(TicketStatus.OPEN);
            assertThat(tiket.getHistory().get(0).getNewStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
            assertThat(tiket.getHistory().get(0).getChangedBy()).isEqualTo(agent);
        }

        @Test
        @DisplayName("preskakanje koraka u zivotnom ciklusu se odbija")
        void nedozvoljenPrelaz() {
            Ticket tiket = ticket(1L, pera, TicketStatus.NEW);
            given(ticketRepository.findByIdWithRelations(1L)).willReturn(Optional.of(tiket));
            given(userService.findEntityById(agent.getId())).willReturn(agent);

            assertThatThrownBy(() -> ticketService.changeStatus(
                    1L, new ChangeStatusRequest(TicketStatus.RESOLVED, null), agentPrincipal))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("nije dozvoljen");

            assertThat(tiket.getStatus()).isEqualTo(TicketStatus.NEW);
            assertThat(tiket.getHistory()).isEmpty();
        }

        @Test
        @DisplayName("postavljanje statusa koji tiket vec ima se odbija")
        void istiStatus() {
            Ticket tiket = ticket(1L, pera, TicketStatus.OPEN);
            given(ticketRepository.findByIdWithRelations(1L)).willReturn(Optional.of(tiket));
            given(userService.findEntityById(agent.getId())).willReturn(agent);

            assertThatThrownBy(() -> ticketService.changeStatus(
                    1L, new ChangeStatusRequest(TicketStatus.OPEN, null), agentPrincipal))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("vec u statusu");
        }

        @Test
        @DisplayName("zatvaranje tiketa upisuje vreme zatvaranja")
        void zatvaranjeUpisujeVreme() {
            Ticket tiket = ticket(1L, pera, TicketStatus.RESOLVED);
            given(ticketRepository.findByIdWithRelations(1L)).willReturn(Optional.of(tiket));
            given(userService.findEntityById(agent.getId())).willReturn(agent);
            given(commentRepository.findByTicketIdOrderByCreatedAtAsc(1L)).willReturn(List.of());

            ticketService.changeStatus(1L, new ChangeStatusRequest(TicketStatus.CLOSED, "Zavrseno"), agentPrincipal);

            assertThat(tiket.getClosedAt()).isNotNull();
        }

        @Test
        @DisplayName("prijavilac moze ponovo da otvori sopstveni resen tiket")
        void prijavilacPonovoOtvara() {
            Ticket tiket = ticket(1L, pera, TicketStatus.RESOLVED);
            given(ticketRepository.findByIdWithRelations(1L)).willReturn(Optional.of(tiket));
            given(userService.findEntityById(pera.getId())).willReturn(pera);
            given(commentRepository.findByTicketIdAndInternalFalseOrderByCreatedAtAsc(1L)).willReturn(List.of());

            TicketResponse odgovor = ticketService.reopen(1L, "Problem se ponovio", peraPrincipal);

            assertThat(odgovor.status()).isEqualTo(TicketStatus.REOPENED);
        }

        @Test
        @DisplayName("tudji tiket ne moze da otvori drugi korisnik")
        void tudjiTiketSeNeMozeOtvoriti() {
            Ticket tiket = ticket(1L, pera, TicketStatus.RESOLVED);
            given(ticketRepository.findByIdWithRelations(1L)).willReturn(Optional.of(tiket));

            assertThatThrownBy(() -> ticketService.reopen(1L, null, mikaPrincipal))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("Kreiranje i dodela")
    class KreiranjeIDodela {

        @Test
        @DisplayName("novi tiket dobija prijavioca iz tokena i status NEW")
        void kreiranjeTiketa() {
            given(categoryRepository.findById(1L)).willReturn(Optional.of(kategorija));
            given(priorityRepository.findById(2L)).willReturn(Optional.of(prioritet));
            given(userService.findEntityById(pera.getId())).willReturn(pera);
            given(ticketRepository.save(any(Ticket.class))).willAnswer(poziv -> poziv.getArgument(0));

            TicketResponse odgovor = ticketService.create(
                    new CreateTicketRequest("Ne radi stampac", "Stampac ne reaguje na komande", 1L, 2L),
                    peraPrincipal);

            assertThat(odgovor.status()).isEqualTo(TicketStatus.NEW);
            assertThat(odgovor.reporter().username()).isEqualTo("pera");
            assertThat(odgovor.assignee()).isNull();
        }

        @Test
        @DisplayName("tiket se ne moze prijaviti u povucenoj kategoriji")
        void neaktivnaKategorija() {
            kategorija.setActive(false);
            given(categoryRepository.findById(1L)).willReturn(Optional.of(kategorija));

            assertThatThrownBy(() -> ticketService.create(
                    new CreateTicketRequest("Naslov problema", "Opis problema koji je dovoljno dug", 1L, 2L),
                    peraPrincipal))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("vise nije u upotrebi");

            verify(ticketRepository, never()).save(any());
        }

        @Test
        @DisplayName("dodela agentu prebacuje tiket iz NEW u OPEN")
        void dodelaMenjaStatus() {
            Ticket tiket = ticket(1L, pera, TicketStatus.NEW);
            given(ticketRepository.findByIdWithRelations(1L)).willReturn(Optional.of(tiket));
            given(userRepository.findById(agent.getId())).willReturn(Optional.of(agent));
            given(userService.findEntityById(agent.getId())).willReturn(agent);
            given(commentRepository.findByTicketIdOrderByCreatedAtAsc(1L)).willReturn(List.of());

            TicketResponse odgovor = ticketService.assign(
                    1L, new AssignTicketRequest(agent.getId()), agentPrincipal);

            assertThat(odgovor.status()).isEqualTo(TicketStatus.OPEN);
            assertThat(odgovor.assignee().username()).isEqualTo("agent1");
        }

        @Test
        @DisplayName("tiket se ne moze dodeliti nalogu koji nije agent")
        void dodelaObicnomKorisniku() {
            Ticket tiket = ticket(1L, pera, TicketStatus.NEW);
            given(ticketRepository.findByIdWithRelations(1L)).willReturn(Optional.of(tiket));
            given(userRepository.findById(mika.getId())).willReturn(Optional.of(mika));

            assertThatThrownBy(() -> ticketService.assign(
                    1L, new AssignTicketRequest(mika.getId()), agentPrincipal))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("samo agentu");
        }

        @Test
        @DisplayName("nepostojeci tiket vraca gresku 'nije pronadjen'")
        void nepostojeciTiket() {
            given(ticketRepository.findByIdWithRelations(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> ticketService.getById(999L, agentPrincipal))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ------------------------------------------------------------------
    // Pomocne metode
    // ------------------------------------------------------------------

    private User user(Long id, String username, String fullName, Role role) {
        User user = new User(username, username + "@test.rs", fullName, "hes", role);
        user.setId(id);
        return user;
    }

    private UserPrincipal principal(User user) {
        return new UserPrincipal(user.getId(), user.getUsername(), user.getPassword(),
                user.getFullName(), user.getRole(), true);
    }

    private Ticket ticket(Long id, User reporter, TicketStatus status) {
        Ticket ticket = new Ticket("Naslov", "Opis problema", reporter, kategorija, prioritet);
        ticket.setId(id);
        ticket.setStatus(status);
        ticket.setCreatedAt(java.time.LocalDateTime.now().minusHours(1));
        ticket.setUpdatedAt(java.time.LocalDateTime.now().minusHours(1));
        return ticket;
    }
}
