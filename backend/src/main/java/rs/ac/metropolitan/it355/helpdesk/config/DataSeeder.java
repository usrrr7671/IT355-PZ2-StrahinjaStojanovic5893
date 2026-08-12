package rs.ac.metropolitan.it355.helpdesk.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.metropolitan.it355.helpdesk.model.Category;
import rs.ac.metropolitan.it355.helpdesk.model.Priority;
import rs.ac.metropolitan.it355.helpdesk.model.Role;
import rs.ac.metropolitan.it355.helpdesk.model.User;
import rs.ac.metropolitan.it355.helpdesk.repository.CategoryRepository;
import rs.ac.metropolitan.it355.helpdesk.repository.PriorityRepository;
import rs.ac.metropolitan.it355.helpdesk.repository.UserRepository;

import java.util.List;

/**
 * Puni bazu pocetnim podacima pri prvom pokretanju: sifarnici kategorija i
 * prioriteta i po jedan nalog za svaku ulogu, da bi aplikacija odmah bila upotrebljiva
 * i da bi demonstracija RBAC-a mogla da se izvede bez rucnog kreiranja naloga.
 *
 * Svaki blok se izvrsava samo ako je odgovarajuca tabela prazna, pa ponovno
 * pokretanje aplikacije ne duplira podatke.
 */
@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PriorityRepository priorityRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      CategoryRepository categoryRepository,
                      PriorityRepository priorityRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.priorityRepository = priorityRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedCategories();
        seedPriorities();
        seedUsers();
    }

    private void seedCategories() {
        if (categoryRepository.count() > 0) {
            return;
        }
        categoryRepository.saveAll(List.of(
                new Category("Hardver", "Kvarovi racunara, stampaca i ostale opreme"),
                new Category("Softver", "Instalacija, greske i licenciranje programa"),
                new Category("Mreza", "Internet konekcija, VPN i pristup deljenim resursima"),
                new Category("Nalozi i pristup", "Zaboravljene lozinke i dodela ovlascenja"),
                new Category("Ostalo", "Zahtevi koji ne pripadaju nijednoj drugoj kategoriji")));
        log.info("Ubacene pocetne kategorije tiketa");
    }

    private void seedPriorities() {
        if (priorityRepository.count() > 0) {
            return;
        }
        priorityRepository.saveAll(List.of(
                new Priority("Nizak", 1, 72),
                new Priority("Srednji", 2, 24),
                new Priority("Visok", 3, 8),
                new Priority("Kriticno", 4, 2)));
        log.info("Ubaceni pocetni prioriteti");
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            return;
        }
        userRepository.saveAll(List.of(
                account("admin", "admin@helpdesk.rs", "Administrator Sistema", Role.ADMIN),
                account("agent1", "marko.agent@helpdesk.rs", "Marko Markovic", Role.AGENT),
                account("agent2", "jelena.agent@helpdesk.rs", "Jelena Jelic", Role.AGENT),
                account("pera", "pera@example.com", "Petar Peric", Role.USER),
                account("mika", "mika@example.com", "Mihajlo Mihajlovic", Role.USER)));

        log.info("Ubaceni pocetni nalozi (lozinka za sve demo naloge: 'lozinka123')");
    }

    private User account(String username, String email, String fullName, Role role) {
        return new User(username, email, fullName, passwordEncoder.encode("lozinka123"), role);
    }
}
