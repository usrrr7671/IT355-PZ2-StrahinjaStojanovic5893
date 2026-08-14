package rs.ac.metropolitan.it355.helpdesk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.ac.metropolitan.it355.helpdesk.dto.ConversationResponse;
import rs.ac.metropolitan.it355.helpdesk.dto.MessageResponse;
import rs.ac.metropolitan.it355.helpdesk.dto.SendMessageRequest;
import rs.ac.metropolitan.it355.helpdesk.dto.StartConversationRequest;
import rs.ac.metropolitan.it355.helpdesk.security.UserPrincipal;
import rs.ac.metropolitan.it355.helpdesk.service.MessageService;

import java.util.List;
import java.util.Map;

/**
 * Prepiska izmedju korisnika i agenta - posebna funkcionalnost projekta.
 *
 * Ni jedna metoda ovde nema {@code @PreAuthorize}, i to je namerno: pristup se ne
 * odredjuje ulogom nego ucescem u razgovoru. Administrator, koji inace sme sve,
 * ovde nema nikakvu prednost - tudju prepisku ne moze da otvori.
 */
@RestController
@RequestMapping("/api/conversations")
@Tag(name = "Prepiska", description = "Privatne poruke izmedju korisnika i osoblja podrske")
public class ConversationController {

    private final MessageService messageService;

    public ConversationController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    @Operation(summary = "Moje prepiske",
            description = "Lista razgovora prijavljenog korisnika, sa najavom poslednje poruke i brojem neprocitanih.")
    public ResponseEntity<List<ConversationResponse>> listMine(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(messageService.listMine(principal));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Broj neprocitanih poruka",
            description = "Ukupan broj neprocitanih poruka u svim prepiskama - znacka u meniju.")
    public ResponseEntity<Map<String, Long>> unreadCount(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(Map.of("unreadCount", messageService.unreadCount(principal)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Otvaranje prepiske",
            description = """
                    Vraca sve poruke i usput obelezava tudje poruke kao procitane.
                    Pristup ima iskljucivo ucesnik razgovora - ni administrator ne moze
                    da otvori prepisku u kojoj ne ucestvuje.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prepiska je otvorena"),
            @ApiResponse(responseCode = "403", description = "Niste ucesnik ove prepiske"),
            @ApiResponse(responseCode = "404", description = "Prepiska ne postoji")
    })
    public ResponseEntity<ConversationResponse> getById(@PathVariable Long id,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(messageService.getById(id, principal));
    }

    @PostMapping
    @Operation(summary = "Zapocinjanje prepiske",
            description = """
                    Ako prepiska sa datim sagovornikom vec postoji, vraca se postojeca.
                    Krajnji korisnik sme da pise samo agentima i administratorima.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Prepiska je zapoceta ili vec postoji"),
            @ApiResponse(responseCode = "403", description = "Korisnik pokusava da pise drugom korisniku")
    })
    public ResponseEntity<ConversationResponse> start(@Valid @RequestBody StartConversationRequest request,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(messageService.start(request, principal));
    }

    @PostMapping("/{id}/messages")
    @Operation(summary = "Slanje poruke u postojecu prepisku")
    public ResponseEntity<MessageResponse> sendMessage(@PathVariable Long id,
                                                       @Valid @RequestBody SendMessageRequest request,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(messageService.sendMessage(id, request, principal));
    }
}
