package rs.ac.metropolitan.it355.helpdesk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.ac.metropolitan.it355.helpdesk.dto.ChangePasswordRequest;
import rs.ac.metropolitan.it355.helpdesk.dto.UserResponse;
import rs.ac.metropolitan.it355.helpdesk.security.UserPrincipal;
import rs.ac.metropolitan.it355.helpdesk.service.UserService;

import java.util.List;

/**
 * Operacije nad nalozima koje nisu administrativne.
 *
 * Lista agenata je namerno dostupna svakom prijavljenom korisniku - bez nje ne bi
 * imao kome da posalje poruku. Vraca se skraceni prikaz, bez e-adresa i datuma
 * kreiranja naloga, jer ti podaci za tu svrhu nisu potrebni.
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Korisnici", description = "Lista agenata i izmena sopstvene lozinke")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/agents")
    @Operation(summary = "Lista aktivnih agenata",
            description = "Koristi je forma za zapocinjanje prepiske sa podrskom.")
    public ResponseEntity<List<UserResponse>> agents() {
        return ResponseEntity.ok(userService.listAgents());
    }

    @PostMapping("/me/change-password")
    @Operation(summary = "Izmena sopstvene lozinke",
            description = """
                    Trazi se i trenutna lozinka, da neko ko zatekne tudju otvorenu sesiju
                    ne bi mogao da preuzme nalog prostom promenom lozinke.
                    """)
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        userService.changePassword(principal.getId(), request);
        return ResponseEntity.noContent().build();
    }
}
