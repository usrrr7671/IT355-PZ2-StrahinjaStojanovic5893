package rs.ac.metropolitan.it355.helpdesk.controller;

import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.ac.metropolitan.it355.helpdesk.dto.CreateUserRequest;
import rs.ac.metropolitan.it355.helpdesk.dto.PageResponse;
import rs.ac.metropolitan.it355.helpdesk.dto.UpdateUserRequest;
import rs.ac.metropolitan.it355.helpdesk.dto.UserResponse;
import rs.ac.metropolitan.it355.helpdesk.model.Role;
import rs.ac.metropolitan.it355.helpdesk.security.UserPrincipal;
import rs.ac.metropolitan.it355.helpdesk.service.UserService;

import java.util.function.Function;

/**
 * Administrativni panel za naloge.
 *
 * Cela putanja /api/admin/** je u SecurityFilterChain-u zatvorena za ulogu ADMIN.
 * Anotacija {@code @PreAuthorize} na nivou klase je namerno ponovljena: ako se
 * jednog dana promeni mapiranje putanje, zastita ostaje uz kod koji stiti.
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administracija naloga", description = "Upravljanje korisnicima - iskljucivo za administratora")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Lista naloga",
            description = "Podrzava pretragu po korisnickom imenu, imenu i e-adresi, kao i filter po ulozi.")
    public ResponseEntity<PageResponse<UserResponse>> list(
            @RequestParam(required = false) String term,
            @RequestParam(required = false) Role role,
            @PageableDefault(size = 20, sort = "username", direction = Sort.Direction.ASC) Pageable pageable) {

        return ResponseEntity.ok(
                PageResponse.of(userService.list(term, role, pageable), Function.identity()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Jedan nalog")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Kreiranje naloga",
            description = "Za razliku od javne registracije, ovde se zadaje uloga - tako nastaju nalozi agenata.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Nalog je kreiran"),
            @ApiResponse(responseCode = "409", description = "Korisnicko ime ili e-adresa su zauzeti")
    })
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Izmena naloga",
            description = """
                    Administrator ne moze sam sebi da promeni ulogu niti da deaktivira
                    sopstveni nalog - inace bi ostao zakljucan van sistema.
                    """)
    public ResponseEntity<UserResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody UpdateUserRequest request,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.update(id, request, principal.getId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deaktivacija naloga",
            description = """
                    Nalozi se ne brisu nego deaktiviraju, da se ne izgubi veza sa
                    tiketima, komentarima i porukama koje je taj nalog ostavio.
                    """)
    public ResponseEntity<Void> deactivate(@PathVariable Long id,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        userService.deactivate(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
