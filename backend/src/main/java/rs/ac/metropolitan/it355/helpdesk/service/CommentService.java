package rs.ac.metropolitan.it355.helpdesk.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.metropolitan.it355.helpdesk.dto.CommentResponse;
import rs.ac.metropolitan.it355.helpdesk.dto.CreateCommentRequest;
import rs.ac.metropolitan.it355.helpdesk.exception.BusinessRuleException;
import rs.ac.metropolitan.it355.helpdesk.exception.ResourceNotFoundException;
import rs.ac.metropolitan.it355.helpdesk.model.Comment;
import rs.ac.metropolitan.it355.helpdesk.model.Role;
import rs.ac.metropolitan.it355.helpdesk.model.Ticket;
import rs.ac.metropolitan.it355.helpdesk.model.User;
import rs.ac.metropolitan.it355.helpdesk.repository.CommentRepository;
import rs.ac.metropolitan.it355.helpdesk.security.UserPrincipal;

import java.util.List;

/**
 * Komentari na tiketu - javna prepiska izmedju korisnika i podrske, plus interne
 * beleske koje osoblje ostavlja jedno drugom.
 *
 * Vidljivost internih beleski je najosetljivije pravilo u aplikaciji, pa se sprovodi
 * na dva mesta: kod pisanja (obican korisnik ne moze da oznaci komentar kao interni)
 * i kod citanja (upit uopste ne dovlaci interne komentare korisniku koji nije osoblje).
 */
@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final TicketService ticketService;
    private final UserService userService;

    public CommentService(CommentRepository commentRepository,
                          TicketService ticketService,
                          UserService userService) {
        this.commentRepository = commentRepository;
        this.ticketService = ticketService;
        this.userService = userService;
    }

    /**
     * Lista komentara jednog tiketa, filtrirana prema ulozi onoga ko cita.
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> listForTicket(Long ticketId, UserPrincipal principal) {
        Ticket ticket = ticketService.findTicketOrThrow(ticketId);
        ticketService.assertCanView(ticket, principal);

        List<Comment> comments = principal.isStaff()
                ? commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                : commentRepository.findByTicketIdAndInternalFalseOrderByCreatedAtAsc(ticketId);

        return comments.stream().map(CommentResponse::from).toList();
    }

    /**
     * Dodaje komentar na tiket.
     *
     * Zahtev za internu belesku od strane obicnog korisnika se odbija sa 403, a ne
     * tiho pretvara u javni komentar: korisnik bi inace mislio da je ostavio belesku
     * koju drugi ne vide, dok bi ona zapravo bila vidljiva svima na tiketu.
     */
    @Transactional
    public CommentResponse addComment(Long ticketId, CreateCommentRequest request, UserPrincipal principal) {
        Ticket ticket = ticketService.findTicketOrThrow(ticketId);
        ticketService.assertCanView(ticket, principal);

        boolean internal = Boolean.TRUE.equals(request.internal());
        if (internal && !principal.isStaff()) {
            throw new AccessDeniedException("Interne beleske moze da pise samo osoblje podrske");
        }

        if (ticket.getStatus().isTerminal() && !principal.isStaff()) {
            throw new BusinessRuleException(
                    "Tiket je zatvoren. Ako problem i dalje postoji, ponovo ga otvorite.");
        }

        User author = userService.findEntityById(principal.getId());
        Comment comment = new Comment(ticket, author, request.content(), internal);
        ticket.addComment(comment);

        return CommentResponse.from(commentRepository.save(comment));
    }

    /**
     * Brisanje komentara. Autor sme da obrise svoj komentar, administrator bilo ciji;
     * agent ne sme da brise tudje komentare, jer bi tako mogao da ukloni prigovor korisnika.
     */
    @Transactional
    public void deleteComment(Long commentId, UserPrincipal principal) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Komentar", commentId));

        boolean isAuthor = comment.getAuthor().getId().equals(principal.getId());
        boolean isAdmin = principal.getRole() == Role.ADMIN;

        if (!isAuthor && !isAdmin) {
            throw new AccessDeniedException("Mozete obrisati samo sopstvene komentare");
        }

        commentRepository.delete(comment);
    }
}
