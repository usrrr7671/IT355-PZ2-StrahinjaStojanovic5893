package rs.ac.metropolitan.it355.helpdesk.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.metropolitan.it355.helpdesk.repository.UserRepository;

/**
 * Most izmedju Spring Security-ja i nase tabele korisnika.
 *
 * AuthenticationManager preko ove klase dobavlja nalog po korisnickom imenu,
 * a zatim DaoAuthenticationProvider poredi prosledjenu lozinku sa BCrypt hesom iz baze.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(UserPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Korisnik sa korisnickim imenom '" + username + "' ne postoji"));
    }
}
