package rs.ac.metropolitan.it355.helpdesk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.ac.metropolitan.it355.helpdesk.dto.CommentResponse;
import rs.ac.metropolitan.it355.helpdesk.dto.CreateCommentRequest;
import rs.ac.metropolitan.it355.helpdesk.security.UserPrincipal;
import rs.ac.metropolitan.it355.helpdesk.service.CommentService;

import java.util.List;

/**
 * Komentari na tiketu.
 *
 * Putanje su ugnjezdene pod tiketom jer komentar bez tiketa ne postoji, dok brisanje
 * ide preko sopstvenog id-ja - komentar je tada vec jednoznacno odredjen.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Komentari", description = "Javni komentari na tiketu i interne beleske podrske")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/tickets/{ticketId}/comments")
    @Operation(summary = "Komentari jednog tiketa",
            description = "Korisnik dobija samo javne komentare; interne beleske vidi iskljucivo osoblje podrske.")
    public ResponseEntity<List<CommentResponse>> list(@PathVariable Long ticketId,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(commentService.listForTicket(ticketId, principal));
    }

    @PostMapping("/tickets/{ticketId}/comments")
    @Operation(summary = "Dodavanje komentara",
            description = """
                    Polje 'internal' oznacava internu belesku. Ako ga posalje korisnik koji
                    nije agent ili administrator, zahtev se odbija sa 403 - a ne pretvara
                    tiho u javni komentar, jer bi korisnik mislio da je napisao nesto skriveno.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Komentar je dodat"),
            @ApiResponse(responseCode = "403", description = "Nema pristup tiketu ili trazi internu belesku bez prava")
    })
    public ResponseEntity<CommentResponse> add(@PathVariable Long ticketId,
                                               @Valid @RequestBody CreateCommentRequest request,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.addComment(ticketId, request, principal));
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "Brisanje komentara",
            description = """
                    Autor sme da obrise svoj komentar, administrator bilo ciji. Agent ne sme
                    da brise tudje komentare, jer bi tako mogao da ukloni prigovor korisnika.
                    """)
    public ResponseEntity<Void> delete(@PathVariable Long commentId,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        commentService.deleteComment(commentId, principal);
        return ResponseEntity.noContent().build();
    }
}
