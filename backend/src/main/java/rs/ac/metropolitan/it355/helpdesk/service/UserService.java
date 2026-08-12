package rs.ac.metropolitan.it355.helpdesk.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.metropolitan.it355.helpdesk.dto.ChangePasswordRequest;
import rs.ac.metropolitan.it355.helpdesk.dto.CreateUserRequest;
import rs.ac.metropolitan.it355.helpdesk.dto.UpdateUserRequest;
import rs.ac.metropolitan.it355.helpdesk.dto.UserResponse;
import rs.ac.metropolitan.it355.helpdesk.exception.BusinessRuleException;
import rs.ac.metropolitan.it355.helpdesk.exception.DuplicateResourceException;
import rs.ac.metropolitan.it355.helpdesk.exception.ResourceNotFoundException;
import rs.ac.metropolitan.it355.helpdesk.model.Role;
import rs.ac.metropolitan.it355.helpdesk.model.User;
import rs.ac.metropolitan.it355.helpdesk.repository.UserRepository;

import java.util.List;

/**
 * Upravljanje nalozima. Vecinu ovih operacija sme da izvrsi samo administrator,
 * sto se obezbedjuje na nivou ruta u SecurityConfig-u i anotacijama u kontroleru.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return UserResponse.from(findUserOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(String term, Role role, Pageable pageable) {
        Page<User> page;
        if (term != null && !term.isBlank()) {
            page = userRepository.search(term.trim(), pageable);
        } else if (role != null) {
            page = userRepository.findByRole(role, pageable);
        } else {
            page = userRepository.findAll(pageable);
        }
        return page.map(UserResponse::from);
    }

    /** Lista agenata - potrebna je i korisniku, da bi znao kome moze da posalje poruku. */
    @Transactional(readOnly = true)
    public List<UserResponse> listAgents() {
        return userRepository.findByRole(Role.AGENT).stream()
                .filter(User::isActive)
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
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
                passwordEncoder.encode(request.password()),
                request.role());

        return UserResponse.from(userRepository.save(user));
    }

    /**
     * Izmena naloga. Polja koja nisu poslata ostaju nepromenjena.
     *
     * @param actingAdminId id administratora koji vrsi izmenu - koristi se da bi se
     *                      sprecilo da administrator sam sebi oduzme ovlascenja ili
     *                      deaktivira nalog i tako ostane zakljucan van sistema
     */
    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request, Long actingAdminId) {
        User user = findUserOrThrow(id);

        if (request.email() != null && !request.email().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new DuplicateResourceException("Nalog sa e-adresom '" + request.email() + "' vec postoji");
            }
            user.setEmail(request.email());
        }
        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName());
        }
        if (request.role() != null && request.role() != user.getRole()) {
            if (user.getId().equals(actingAdminId)) {
                throw new BusinessRuleException("Ne mozete sami sebi promeniti ulogu");
            }
            user.setRole(request.role());
        }
        if (request.active() != null && request.active() != user.isActive()) {
            if (user.getId().equals(actingAdminId)) {
                throw new BusinessRuleException("Ne mozete deaktivirati sopstveni nalog");
            }
            user.setActive(request.active());
        }

        return UserResponse.from(user);   // izmene se upisuju automatski na kraju transakcije
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUserOrThrow(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessRuleException("Trenutna lozinka nije ispravna");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessRuleException("Nova lozinka mora biti razlicita od trenutne");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
    }

    /**
     * Nalozi se ne brisu, vec deaktiviraju - brisanjem bi se izgubila veza sa
     * tiketima, komentarima i porukama koje je taj nalog ostavio.
     */
    @Transactional
    public void deactivate(Long id, Long actingAdminId) {
        if (id.equals(actingAdminId)) {
            throw new BusinessRuleException("Ne mozete deaktivirati sopstveni nalog");
        }
        User user = findUserOrThrow(id);
        user.setActive(false);
    }

    /** Vraca entitet - koriste ga drugi servisi kojima treba upravljani objekat, a ne DTO. */
    @Transactional(readOnly = true)
    public User findEntityById(Long id) {
        return findUserOrThrow(id);
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Korisnik", id));
    }

    /** Agent sa najmanje otvorenih tiketa, ako uopste postoji aktivan agent. */
    @Transactional(readOnly = true)
    public User findLeastBusyAgent() {
        List<User> agents = userRepository.findAgentsOrderedByWorkload(PageRequest.of(0, 1));
        return agents.isEmpty() ? null : agents.get(0);
    }
}
