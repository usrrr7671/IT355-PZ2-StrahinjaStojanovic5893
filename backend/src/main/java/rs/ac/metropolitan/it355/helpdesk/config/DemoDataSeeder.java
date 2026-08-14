package rs.ac.metropolitan.it355.helpdesk.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.metropolitan.it355.helpdesk.model.Category;
import rs.ac.metropolitan.it355.helpdesk.model.Comment;
import rs.ac.metropolitan.it355.helpdesk.model.Conversation;
import rs.ac.metropolitan.it355.helpdesk.model.Message;
import rs.ac.metropolitan.it355.helpdesk.model.Priority;
import rs.ac.metropolitan.it355.helpdesk.model.Ticket;
import rs.ac.metropolitan.it355.helpdesk.model.TicketStatus;
import rs.ac.metropolitan.it355.helpdesk.model.TicketStatusHistory;
import rs.ac.metropolitan.it355.helpdesk.model.User;
import rs.ac.metropolitan.it355.helpdesk.repository.CategoryRepository;
import rs.ac.metropolitan.it355.helpdesk.repository.ConversationRepository;
import rs.ac.metropolitan.it355.helpdesk.repository.PriorityRepository;
import rs.ac.metropolitan.it355.helpdesk.repository.TicketRepository;
import rs.ac.metropolitan.it355.helpdesk.repository.UserRepository;

import java.time.LocalDateTime;

/**
 * Demonstracioni tiketi, komentari i prepiska.
 *
 * Odvojen je od {@link DataSeeder} jer resava drugi problem: {@code DataSeeder}
 * puni ono bez cega aplikacija ne moze da radi (sifarnici i nalozi), a ovaj klasa
 * dodaje sadrzaj koji cini prikaz smislenim - listu tiketa u raznim statusima,
 * internu belesku i zapocetu prepisku.
 *
 * Pokrece se samo kada u bazi nema nijednog tiketa.
 */
@Component
@Order(2)
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final TicketRepository ticketRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PriorityRepository priorityRepository;

    public DemoDataSeeder(TicketRepository ticketRepository,
                          ConversationRepository conversationRepository,
                          UserRepository userRepository,
                          CategoryRepository categoryRepository,
                          PriorityRepository priorityRepository) {
        this.ticketRepository = ticketRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.priorityRepository = priorityRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (ticketRepository.count() > 0) {
            return;
        }

        User pera = user("pera");
        User mika = user("mika");
        User agent1 = user("agent1");
        User agent2 = user("agent2");

        Category hardver = category("Hardver");
        Category softver = category("Softver");
        Category mreza = category("Mreza");
        Category nalozi = category("Nalozi i pristup");

        Priority nizak = priority("Nizak");
        Priority srednji = priority("Srednji");
        Priority visok = priority("Visok");
        Priority kriticno = priority("Kriticno");

        // 1) Novoprijavljen tiket koji jos niko nije preuzeo.
        Ticket t1 = ticket("Monitor treperi na radnoj stanici RS-14",
                "Slika na monitoru treperi svakih nekoliko sekundi. Zamena kabla nije pomogla.",
                pera, hardver, srednji);
        history(t1, pera, null, TicketStatus.NEW, "Tiket je prijavljen");

        // 2) Tiket u radu, sa javnim komentarom i internom beleskom podrske.
        Ticket t2 = ticket("Ne mogu da se prijavim na VPN",
                "Od jutros VPN klijent javlja gresku 'authentication failed', a lozinka je ispravna.",
                mika, mreza, visok);
        t2.setAssignee(agent1);
        t2.setStatus(TicketStatus.IN_PROGRESS);
        history(t2, mika, null, TicketStatus.NEW, "Tiket je prijavljen");
        history(t2, agent1, TicketStatus.NEW, TicketStatus.OPEN, "Tiket je dodeljen agentu Marko Markovic");
        history(t2, agent1, TicketStatus.OPEN, TicketStatus.IN_PROGRESS, "Proveravam podesavanja naloga");
        comment(t2, agent1, "Dobar dan, proveravam vas nalog na VPN serveru. Javljam se u toku dana.", false);
        comment(t2, agent1, "Nalog je zakljucan posle vise neuspelih pokusaja - traziti odobrenje za otkljucavanje.", true);
        comment(t2, mika, "Hvala, cekam odgovor.", false);

        // 3) Resen tiket koji ceka potvrdu korisnika.
        Ticket t3 = ticket("Excel se gasi pri otvaranju velikih tabela",
                "Prilikom otvaranja izvestaja od oko 50.000 redova aplikacija se zatvori bez poruke.",
                pera, softver, srednji);
        t3.setAssignee(agent2);
        t3.setStatus(TicketStatus.RESOLVED);
        history(t3, pera, null, TicketStatus.NEW, "Tiket je prijavljen");
        history(t3, agent2, TicketStatus.NEW, TicketStatus.OPEN, "Tiket je dodeljen agentu Jelena Jelic");
        history(t3, agent2, TicketStatus.OPEN, TicketStatus.IN_PROGRESS, "Reprodukujem problem");
        history(t3, agent2, TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED, "Povecana kolicina memorije dodeljena aplikaciji");
        comment(t3, agent2, "Problem je bio u ogranicenju memorije. Molim vas da probate ponovo.", false);

        // 4) Zatvoren tiket - zavrsen slucaj u istoriji.
        Ticket t4 = ticket("Zaboravljena lozinka za domenski nalog",
                "Ne mogu da se prijavim na racunar, lozinka je istekla tokom godisnjeg odmora.",
                mika, nalozi, kriticno);
        t4.setAssignee(agent1);
        t4.setStatus(TicketStatus.CLOSED);
        t4.setClosedAt(LocalDateTime.now().minusDays(1));
        history(t4, mika, null, TicketStatus.NEW, "Tiket je prijavljen");
        history(t4, agent1, TicketStatus.NEW, TicketStatus.OPEN, "Tiket je dodeljen agentu Marko Markovic");
        history(t4, agent1, TicketStatus.OPEN, TicketStatus.RESOLVED, "Lozinka resetovana i poslata na alternativnu adresu");
        history(t4, agent1, TicketStatus.RESOLVED, TicketStatus.CLOSED, "Korisnik je potvrdio da moze da se prijavi");
        comment(t4, agent1, "Lozinka je resetovana. Pri prvoj prijavi bicete zatrazeni da je promenite.", false);

        // 5) Tiket niskog prioriteta, tek prijavljen.
        Ticket t5 = ticket("Zahtev za instalaciju programa za obradu slika",
                "Za potrebe pripreme materijala potreban mi je program za osnovnu obradu slika.",
                pera, softver, nizak);
        history(t5, pera, null, TicketStatus.NEW, "Tiket je prijavljen");

        ticketRepository.saveAll(java.util.List.of(t1, t2, t3, t4, t5));

        // Prepiska povodom tiketa sa VPN-om.
        Conversation razgovor = new Conversation(mika, agent1, t2);
        message(razgovor, mika, "Dobar dan, da li ima novosti oko VPN naloga?");
        message(razgovor, agent1, "Dobar dan, nalog je zakljucan iz bezbednosnih razloga. Saljem zahtev za otkljucavanje.");
        message(razgovor, mika, "Hvala, koliko to obicno traje?");
        conversationRepository.save(razgovor);

        log.info("Ubaceni demonstracioni tiketi, komentari i prepiska");
    }

    // ------------------------------------------------------------------
    // Pomocne metode za citljivije kreiranje demo podataka
    // ------------------------------------------------------------------

    private Ticket ticket(String title, String description, User reporter, Category category, Priority priority) {
        return new Ticket(title, description, reporter, category, priority);
    }

    private void comment(Ticket ticket, User author, String content, boolean internal) {
        ticket.addComment(new Comment(ticket, author, content, internal));
    }

    private void history(Ticket ticket, User actor, TicketStatus from, TicketStatus to, String note) {
        ticket.addHistoryEntry(new TicketStatusHistory(ticket, actor, from, to, note));
    }

    private void message(Conversation conversation, User sender, String content) {
        conversation.addMessage(new Message(conversation, sender, content));
    }

    private User user(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Ocekivan demo nalog '" + username + "' ne postoji"));
    }

    private Category category(String name) {
        return categoryRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new IllegalStateException("Ocekivana kategorija '" + name + "' ne postoji"));
    }

    private Priority priority(String name) {
        return priorityRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new IllegalStateException("Ocekivan prioritet '" + name + "' ne postoji"));
    }
}
