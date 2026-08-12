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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.ac.metropolitan.it355.helpdesk.dto.AuthResponse;
import rs.ac.metropolitan.it355.helpdesk.dto.LoginRequest;
import rs.ac.metropolitan.it355.helpdesk.dto.RegisterRequest;
import rs.ac.metropolitan.it355.helpdesk.dto.UserResponse;
import rs.ac.metropolitan.it355.helpdesk.service.AuthService;
import rs.ac.metropolitan.it355.helpdesk.service.UserService;
import rs.ac.metropolitan.it355.helpdesk.security.UserPrincipal;

/**
 * Registracija, prijava i podaci o trenutno prijavljenom nalogu.
 * Jedine dve javne rute u sistemu su /register i /login.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autentifikacija", description = "Registracija, prijava i podaci o prijavljenom korisniku")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/register")
    @Operation(summary = "Registracija novog korisnika",
            description = "Kreira nalog sa ulogom USER i odmah vraca JWT token.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Nalog je kreiran"),
            @ApiResponse(responseCode = "400", description = "Neispravni podaci"),
            @ApiResponse(responseCode = "409", description = "Korisnicko ime ili e-adresa su zauzeti")
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Prijava korisnika",
            description = "Proverava kredencijale i vraca JWT token koji se salje kao 'Authorization: Bearer <token>'.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prijava uspesna"),
            @ApiResponse(responseCode = "401", description = "Pogresno korisnicko ime ili lozinka")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Podaci o prijavljenom korisniku",
            description = "Frontend je poziva pri osvezavanju stranice da bi proverio da li token jos vazi.")
    public ResponseEntity<UserResponse> currentUser(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.getById(principal.getId()));
    }
}
