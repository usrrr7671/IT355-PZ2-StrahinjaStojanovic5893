package rs.ac.metropolitan.it355.helpdesk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.ac.metropolitan.it355.helpdesk.dto.AssignTicketRequest;
import rs.ac.metropolitan.it355.helpdesk.dto.ChangeStatusRequest;
import rs.ac.metropolitan.it355.helpdesk.dto.CreateTicketRequest;
import rs.ac.metropolitan.it355.helpdesk.dto.PageResponse;
import rs.ac.metropolitan.it355.helpdesk.dto.TicketResponse;
import rs.ac.metropolitan.it355.helpdesk.dto.TicketSummaryResponse;
import rs.ac.metropolitan.it355.helpdesk.dto.UpdateTicketRequest;
import rs.ac.metropolitan.it355.helpdesk.model.TicketStatus;
import rs.ac.metropolitan.it355.helpdesk.security.UserPrincipal;
import rs.ac.metropolitan.it355.helpdesk.service.TicketService;

/**
 * Tiketi - srce aplikacije.
 *
 * Zastita je namerno podeljena na dva nivoa. Operacije koje zavise iskljucivo od
 * uloge (promena statusa, dodela agentu, brisanje) zatvorene su anotacijom
 * {@code @PreAuthorize} vec na ulazu u metodu. Operacije koje zavise od toga *ciji*
 * je tiket (citanje, izmena) moraju da udju u servis, jer se odgovor zna tek kada
 * se zapis ucita iz baze.
 */
@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Tiketi", description = "Prijava, pretraga i obrada tiketa podrske")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    @Operation(summary = "Pretraga tiketa",
            description = """
                    Vraca stranicenu listu tiketa sa opcionim filterima.
                    Agent i administrator vide sve tikete, dok korisnik uvek dobija
                    samo sopstvene - filter po prijaviocu mu se postavlja na serveru,
                    bez obzira na parametre koje posalje.
                    """)
    public ResponseEntity<PageResponse<TicketSummaryResponse>> search(
            @Parameter(description = "Filter po statusu") @RequestParam(required = false) TicketStatus status,
            @Parameter(description = "Filter po kategoriji") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Filter po prioritetu") @RequestParam(required = false) Long priorityId,
            @Parameter(description = "Filter po zaduzenom agentu") @RequestParam(required = false) Long assigneeId,
            @Parameter(description = "Pretraga po naslovu i opisu") @RequestParam(required = false) String term,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(ticketService.search(
                status, categoryId, priorityId, assigneeId, term, pageable, principal));
    }

    @GetMapping("/my")
    @Operation(summary = "Moji prijavljeni tiketi")
    public ResponseEntity<PageResponse<TicketSummaryResponse>> myTickets(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(ticketService.myTickets(principal, pageable));
    }

    @GetMapping("/assigned-to-me")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Radni red prijavljenog agenta",
            description = "Tiketi dodeljeni prijavljenom agentu. Dostupno samo osoblju podrske.")
    public ResponseEntity<PageResponse<TicketSummaryResponse>> assignedToMe(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(ticketService.assignedToMe(principal, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalji tiketa",
            description = """
                    Vraca tiket sa komentarima i istorijom statusa. Korisnik u odgovoru
                    ne dobija interne beleske podrske - one se izbacuju vec u upitu.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tiket je pronadjen"),
            @ApiResponse(responseCode = "403", description = "Tiket pripada drugom korisniku"),
            @ApiResponse(responseCode = "404", description = "Tiket ne postoji")
    })
    public ResponseEntity<TicketResponse> getById(@PathVariable Long id,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ticketService.getById(id, principal));
    }

    @PostMapping
    @Operation(summary = "Prijava novog tiketa",
            description = "Prijavilac se uzima iz tokena, pa nije moguce prijaviti tiket u tudje ime.")
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest request,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.create(request, principal));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Izmena tiketa",
            description = """
                    Prijavilac sme da dopuni svoj tiket samo dok je u statusu NEW i ne sme
                    da menja prioritet. Osoblje podrske nema ta ogranicenja.
                    """)
    public ResponseEntity<TicketResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody UpdateTicketRequest request,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ticketService.update(id, request, principal));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Promena statusa tiketa",
            description = """
                    Kljucna RBAC operacija: status menja iskljucivo osoblje podrske.
                    Dozvoljeni su samo prelazi koje propisuje zivotni ciklus tiketa,
                    a svaka promena ostavlja trag u istoriji.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status je promenjen"),
            @ApiResponse(responseCode = "400", description = "Prelaz izmedju statusa nije dozvoljen"),
            @ApiResponse(responseCode = "403", description = "Ulogu USER ova operacija ne dozvoljava")
    })
    public ResponseEntity<TicketResponse> changeStatus(@PathVariable Long id,
                                                       @Valid @RequestBody ChangeStatusRequest request,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ticketService.changeStatus(id, request, principal));
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Dodela tiketa agentu",
            description = "Kada agentId nije poslat, tiket dobija agent sa najmanje otvorenih tiketa.")
    public ResponseEntity<TicketResponse> assign(@PathVariable Long id,
                                                 @RequestBody(required = false) AssignTicketRequest request,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ticketService.assign(id, request, principal));
    }

    @PatchMapping("/{id}/reopen")
    @Operation(summary = "Ponovno otvaranje tiketa",
            description = """
                    Jedina promena statusa koju sme da izvede i obican korisnik, i to
                    samo nad sopstvenim tiketom koji je podrska proglasila resenim.
                    """)
    public ResponseEntity<TicketResponse> reopen(@PathVariable Long id,
                                                 @RequestParam(required = false) String reason,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ticketService.reopen(id, reason, principal));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Brisanje tiketa",
            description = "Rezervisano za administratora. Komentari i istorija se brisu kaskadno.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ticketService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
