package rs.ac.metropolitan.it355.helpdesk.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.metropolitan.it355.helpdesk.dto.AuthResponse;
import rs.ac.metropolitan.it355.helpdesk.dto.LoginRequest;
import rs.ac.metropolitan.it355.helpdesk.dto.RegisterRequest;
import rs.ac.metropolitan.it355.helpdesk.dto.UserResponse;
import rs.ac.metropolitan.it355.helpdesk.exception.DuplicateResourceException;
import rs.ac.metropolitan.it355.helpdesk.model.Role;
import rs.ac.metropolitan.it355.helpdesk.model.User;
import rs.ac.metropolitan.it355.helpdesk.repository.UserRepository;
import rs.ac.metropolitan.it355.helpdesk.security.JwtService;
import rs.ac.metropolitan.it355.helpdesk.security.UserPrincipal;

/**
 * Registracija i prijava korisnika.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * Kreira nalog krajnjeg korisnika i odmah vraca token, da korisnik posle
     * registracije ne bi morao ponovo da unosi podatke na formi za prijavu.
     *
     * Provera zauzetosti korisnickog imena i e-adrese radi se pre upisa, ali je
     * konacna garancija jedinstveni indeks u bazi - transakcija ce se povuci ako
     * dva zahteva istovremeno pokusaju isto korisnicko ime.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Korisnicko ime '" + request.username() + "' je vec zauzeto");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Nalog sa e-adresom '" + request.email() + "' vec postoji");
        }

        User user = new User(
                request.username(),
                request.email(),
                request.fullName(),
                passwordEncoder.encode(request.password()),   // lozinka se cuva iskljucivo kao BCrypt hes
                Role.USER);

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(UserPrincipal.from(saved));
        return AuthResponse.of(token, jwtService.getExpirationMs(), UserResponse.from(saved));
    }

    /**
     * Proverava kredencijale preko AuthenticationManager-a i, ako su ispravni,
     * izdaje JWT token. Ako nisu, Spring Security baca BadCredentialsException,
     * koji globalni handler prevodi u HTTP 401.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId()).orElseThrow();

        String token = jwtService.generateToken(principal);
        return AuthResponse.of(token, jwtService.getExpirationMs(), UserResponse.from(user));
    }
}
