package rs.ac.metropolitan.it355.helpdesk.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.metropolitan.it355.helpdesk.dto.AssignTicketRequest;
import rs.ac.metropolitan.it355.helpdesk.dto.ChangeStatusRequest;
import rs.ac.metropolitan.it355.helpdesk.dto.CommentResponse;
import rs.ac.metropolitan.it355.helpdesk.dto.CreateTicketRequest;
import rs.ac.metropolitan.it355.helpdesk.dto.PageResponse;
import rs.ac.metropolitan.it355.helpdesk.dto.TicketHistoryResponse;
import rs.ac.metropolitan.it355.helpdesk.dto.TicketResponse;
import rs.ac.metropolitan.it355.helpdesk.dto.TicketSummaryResponse;
import rs.ac.metropolitan.it355.helpdesk.dto.UpdateTicketRequest;
import rs.ac.metropolitan.it355.helpdesk.exception.BusinessRuleException;
import rs.ac.metropolitan.it355.helpdesk.exception.ResourceNotFoundException;
import rs.ac.metropolitan.it355.helpdesk.model.Category;
import rs.ac.metropolitan.it355.helpdesk.model.Comment;
import rs.ac.metropolitan.it355.helpdesk.model.Priority;
import rs.ac.metropolitan.it355.helpdesk.model.Role;
import rs.ac.metropolitan.it355.helpdesk.model.Ticket;
import rs.ac.metropolitan.it355.helpdesk.model.TicketStatus;
import rs.ac.metropolitan.it355.helpdesk.model.TicketStatusHistory;
import rs.ac.metropolitan.it355.helpdesk.model.User;
import rs.ac.metropolitan.it355.helpdesk.repository.CategoryRepository;
import rs.ac.metropolitan.it355.helpdesk.repository.CommentRepository;
import rs.ac.metropolitan.it355.helpdesk.repository.ConversationRepository;
import rs.ac.metropolitan.it355.helpdesk.repository.PriorityRepository;
import rs.ac.metropolitan.it355.helpdesk.repository.TicketRepository;
import rs.ac.metropolitan.it355.helpdesk.repository.TicketStatusHistoryRepository;
import rs.ac.metropolitan.it355.helpdesk.repository.UserRepository;
import rs.ac.metropolitan.it355.helpdesk.security.UserPrincipal;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Poslovna logika nad tiketima.
 *
 * Ovde zivi vecina pravila pristupa koja se ne mogu izraziti preko URL-a. Ruta
 * /api/tickets/{id} je ista za sve, ali odgovor na pitanje "smes li da je vidis"
 * zavisi od toga ciji je tiket - a to zna samo sloj koji je ucitao zapis.
 *
 * Pravila su namerno na jednom mestu (metode koje pocinju sa {@code assert...}),
 * da bi se pri odbrani projekta moglo pokazati gde tacno stoji svaka provera.
 */
@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final PriorityRepository priorityRepository;
    private final CommentRepository commentRepository;
    private final TicketStatusHistoryRepository historyRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public TicketService(TicketRepository ticketRepository,
                         CategoryRepository categoryRepository,
                         PriorityRepository priorityRepository,
                         CommentRepository commentRepository,
                         TicketStatusHistoryRepository historyRepository,
                         ConversationRepository conversationRepository,
                         UserRepository userRepository,
                         UserService userService) {
        this.ticketRepository = ticketRepository;
        this.categoryRepository = categoryRepository;
        this.priorityRepository = priorityRepository;
        this.commentRepository = commentRepository;
        this.historyRepository = historyRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    // ==================================================================
    // Citanje
    // ==================================================================

    /**
     * Pretraga tiketa sa opcionim filterima.
     *
     * Korisniku koji nije osoblje podrske filter po prijaviocu se postavlja nasilno,
     * bez obzira na to sta je poslao u zahtevu - time ne moze da izlista tudje
     * tikete tako sto rucno promeni parametre u URL-u.
     */
    @Transactional(readOnly = true)
    public PageResponse<TicketSummaryResponse> search(TicketStatus status,
                                                      Long categoryId,
                                                      Long priorityId,
                                                      Long assigneeId,
                                                      String term,
                                                      Pageable pageable,
                                                      UserPrincipal principal) {

        Long reporterFilter = principal.isStaff() ? null : principal.getId();
        String searchTerm = (term == null || term.isBlank()) ? null : term.trim();

        Page<Ticket> page = ticketRepository.search(
                status, categoryId, priorityId, assigneeId, reporterFilter, searchTerm, pageable);

        return PageResponse.of(page, TicketSummaryResponse::from);
    }

    /** Tiketi koje je prijavio sam korisnik. */
    @Transactional(readOnly = true)
    public PageResponse<TicketSummaryResponse> myTickets(UserPrincipal principal, Pageable pageable) {
        return PageResponse.of(
                ticketRepository.findByReporterId(principal.getId(), pageable),
                TicketSummaryResponse::from);
    }

    /** Tiketi dodeljeni prijavljenom agentu - njegov radni red. */
    @Transactional(readOnly = true)
    public PageResponse<TicketSummaryResponse> assignedToMe(UserPrincipal principal, Pageable pageable) {
        return PageResponse.of(
                ticketRepository.findByAssigneeId(principal.getId(), pageable),
                TicketSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public TicketResponse getById(Long id, UserPrincipal principal) {
        Ticket ticket = findTicketOrThrow(id);
        assertCanView(ticket, principal);

        // Interne beleske se izbacuju vec u upitu, a ne filtriranjem posle ucitavanja -
        // tako sadrzaj koji korisnik ne sme da vidi nikada ne napusti bazu.
        List<Comment> comments = principal.isStaff()
                ? commentRepository.findByTicketIdOrderByCreatedAtAsc(id)
                : commentRepository.findByTicketIdAndInternalFalseOrderByCreatedAtAsc(id);

        List<CommentResponse> commentDtos = comments.stream().map(CommentResponse::from).toList();
        List<TicketHistoryResponse> historyDtos = historyRepository
                .findByTicketIdOrderByChangedAtAsc(id).stream()
                .map(TicketHistoryResponse::from)
                .toList();

        return TicketResponse.from(ticket, commentDtos, historyDtos);
    }

    // ==================================================================
    // Izmene
    // ==================================================================

    /**
     * Kreira tiket. Prijavilac se uzima iz tokena, a ne iz tela zahteva, pa nije
     * moguce prijaviti tiket u tudje ime. Status je uvek NEW i agent nije dodeljen.
     */
    @Transactional
    public TicketResponse create(CreateTicketRequest request, UserPrincipal principal) {
        Category category = findCategoryOrThrow(request.categoryId());
        if (!category.isActive()) {
            throw new BusinessRuleException("Kategorija '" + category.getName() + "' vise nije u upotrebi");
        }
        Priority priority = findPriorityOrThrow(request.priorityId());
        User reporter = userService.findEntityById(principal.getId());

        Ticket ticket = new Ticket(request.title(), request.description(), reporter, category, priority);
        Ticket saved = ticketRepository.save(ticket);

        // Prvi zapis u istoriji, da bi i sam nastanak tiketa imao trag.
        saved.addHistoryEntry(new TicketStatusHistory(
                saved, reporter, null, TicketStatus.NEW, "Tiket je prijavljen"));

        return TicketResponse.from(saved, List.of(), toHistoryDtos(saved));
    }

    /**
     * Izmena sadrzaja tiketa.
     *
     * Prijavilac sme da dopuni svoj tiket samo dok je jos u statusu NEW - kada
     * agent jednom pocne rad, opis problema ne sme da se menja pod njegovim rukama.
     * Prioritet menja iskljucivo osoblje podrske, jer je procena hitnosti njihov
     * posao; u suprotnom bi svaki tiket vremenom postao "kriticno".
     */
    @Transactional
    public TicketResponse update(Long id, UpdateTicketRequest request, UserPrincipal principal) {
        Ticket ticket = findTicketOrThrow(id);
        assertCanView(ticket, principal);

        if (!principal.isStaff()) {
            if (ticket.getStatus() != TicketStatus.NEW) {
                throw new BusinessRuleException(
                        "Tiket vise ne mozete menjati jer je podrska zapocela rad na njemu");
            }
            if (request.priorityId() != null) {
                throw new AccessDeniedException("Prioritet moze da menja samo osoblje podrske");
            }
        }

        if (request.title() != null && !request.title().isBlank()) {
            ticket.setTitle(request.title());
        }
        if (request.description() != null && !request.description().isBlank()) {
            ticket.setDescription(request.description());
        }
        if (request.categoryId() != null) {
            ticket.setCategory(findCategoryOrThrow(request.categoryId()));
        }
        if (request.priorityId() != null) {
            ticket.setPriority(findPriorityOrThrow(request.priorityId()));
        }

        return buildFullResponse(ticket, principal);
    }

    /**
     * Promena statusa tiketa - operacija rezervisana za agente i administratore.
     *
     * Dozvoljeni prelazi se ne proveravaju ovde, vec ih zna sam {@link TicketStatus},
     * pa je nemoguce da se pravilo razidje izmedju servisa i modela.
     */
    @Transactional
    public TicketResponse changeStatus(Long id, ChangeStatusRequest request, UserPrincipal principal) {
        Ticket ticket = findTicketOrThrow(id);
        User actor = userService.findEntityById(principal.getId());

        TicketStatus current = ticket.getStatus();
        TicketStatus target = request.status();

        if (current == target) {
            throw new BusinessRuleException("Tiket je vec u statusu " + target);
        }
        if (!current.canTransitionTo(target)) {
            throw new BusinessRuleException(
                    "Prelaz iz statusa " + current + " u " + target + " nije dozvoljen. Dozvoljeni prelazi: "
                            + current.allowedTransitions());
        }

        applyStatusChange(ticket, actor, target, request.note());
        return buildFullResponse(ticket, principal);
    }

    /**
     * Ponovno otvaranje tiketa od strane prijavioca.
     *
     * Namerno je odvojeno od {@link #changeStatus}, koji je zatvoren za obicnog
     * korisnika: ovo je jedina promena statusa koju prijavilac sme da izvede, i
     * to samo nad sopstvenim tiketom koji je podrska proglasila resenim.
     */
    @Transactional
    public TicketResponse reopen(Long id, String reason, UserPrincipal principal) {
        Ticket ticket = findTicketOrThrow(id);

        if (!isReporter(ticket, principal) && !principal.isStaff()) {
            throw new AccessDeniedException("Tiket moze ponovo da otvori samo onaj ko ga je prijavio");
        }
        if (!ticket.getStatus().canTransitionTo(TicketStatus.REOPENED)) {
            throw new BusinessRuleException(
                    "Tiket u statusu " + ticket.getStatus() + " ne moze biti ponovo otvoren");
        }

        User actor = userService.findEntityById(principal.getId());
        applyStatusChange(ticket, actor, TicketStatus.REOPENED,
                reason == null || reason.isBlank() ? "Korisnik nije zadovoljan resenjem" : reason);

        return buildFullResponse(ticket, principal);
    }

    /**
     * Dodela tiketa agentu. Kada agentId nije poslat, tiket dobija agent sa
     * najmanje otvorenih tiketa, cime se posao ravnomerno raspodeljuje.
     */
    @Transactional
    public TicketResponse assign(Long id, AssignTicketRequest request, UserPrincipal principal) {
        Ticket ticket = findTicketOrThrow(id);

        User agent;
        if (request == null || request.agentId() == null) {
            agent = userService.findLeastBusyAgent();
            if (agent == null) {
                throw new BusinessRuleException("Ne postoji nijedan aktivan agent kome bi tiket bio dodeljen");
            }
        } else {
            agent = userRepository.findById(request.agentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Korisnik", request.agentId()));
            if (agent.getRole() != Role.AGENT && agent.getRole() != Role.ADMIN) {
                throw new BusinessRuleException("Tiket se moze dodeliti samo agentu ili administratoru");
            }
            if (!agent.isActive()) {
                throw new BusinessRuleException("Nalog '" + agent.getUsername() + "' je deaktiviran");
            }
        }

        ticket.setAssignee(agent);

        // Preuzimanjem tiket automatski prelazi iz NEW u OPEN - nedodeljen tiket u
        // statusu OPEN ili dodeljen tiket koji je i dalje NEW bili bi protivrecni.
        if (ticket.getStatus() == TicketStatus.NEW) {
            User actor = userService.findEntityById(principal.getId());
            applyStatusChange(ticket, actor, TicketStatus.OPEN,
                    "Tiket je dodeljen agentu " + agent.getFullName());
        }

        return buildFullResponse(ticket, principal);
    }

    /**
     * Brisanje tiketa je iskljucivo administratorska operacija.
     *
     * Komentari i istorija odlaze kaskadno jer bez tiketa nemaju smisla. Prepiska
     * je drugacija: ona pripada sagovornicima, a ne tiketu, pa se samo odvezuje -
     * poruke koje su ljudi razmenili ne treba da nestanu zato sto je tiket obrisan.
     */
    @Transactional
    public void delete(Long id) {
        Ticket ticket = findTicketOrThrow(id);
        conversationRepository.detachFromTicket(id);
        ticketRepository.delete(ticket);
    }

    // ==================================================================
    // Pravila pristupa
    // ==================================================================

    /**
     * Osoblje podrske vidi sve tikete, a korisnik samo one koje je sam prijavio.
     *
     * Baca se 403 umesto 404 svesno: korisnik koji pogodi tudji id ne treba da
     * dobije potvrdu da taj tiket postoji, ali odgovor mora da bude isti bez obzira
     * na to da li tiket postoji ili ne - zato provera dolazi tek posle ucitavanja.
     */
    void assertCanView(Ticket ticket, UserPrincipal principal) {
        if (principal.isStaff() || isReporter(ticket, principal)) {
            return;
        }
        throw new AccessDeniedException("Nemate pristup ovom tiketu");
    }

    private boolean isReporter(Ticket ticket, UserPrincipal principal) {
        return ticket.getReporter().getId().equals(principal.getId());
    }

    // ==================================================================
    // Pomocne metode
    // ==================================================================

    private void applyStatusChange(Ticket ticket, User actor, TicketStatus target, String note) {
        TicketStatus previous = ticket.getStatus();
        ticket.setStatus(target);

        if (target == TicketStatus.CLOSED) {
            ticket.setClosedAt(LocalDateTime.now());
        } else if (previous == TicketStatus.CLOSED) {
            ticket.setClosedAt(null);   // ponovo otvoren tiket nema datum zatvaranja
        }

        ticket.addHistoryEntry(new TicketStatusHistory(ticket, actor, previous, target, note));
    }

    private TicketResponse buildFullResponse(Ticket ticket, UserPrincipal principal) {
        List<Comment> comments = principal.isStaff()
                ? commentRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId())
                : commentRepository.findByTicketIdAndInternalFalseOrderByCreatedAtAsc(ticket.getId());

        return TicketResponse.from(
                ticket,
                comments.stream().map(CommentResponse::from).toList(),
                toHistoryDtos(ticket));
    }

    private List<TicketHistoryResponse> toHistoryDtos(Ticket ticket) {
        return ticket.getHistory().stream().map(TicketHistoryResponse::from).toList();
    }

    Ticket findTicketOrThrow(Long id) {
        return ticketRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tiket", id));
    }

    private Category findCategoryOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kategorija", id));
    }

    private Priority findPriorityOrThrow(Long id) {
        return priorityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prioritet", id));
    }
}
