package rs.ac.metropolitan.it355.helpdesk.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Prepiska izmedju tacno dva ucesnika (posebna funkcionalnost projekta).
 *
 * Za razliku od komentara, koji su vezani za tiket i vidljivi svima sa pristupom tiketu,
 * prepiska je privatan kanal izmedju korisnika i agenta.
 *
 * Da bi par ucesnika bio jedinstven bez obzira na to ko je prvi zapoceo razgovor,
 * ucesnici se uvek upisuju normalizovano: {@code participantOne} je onaj sa manjim id-jem.
 * Time jedinstveni indeks nad parom stvarno sprecava duple prepiske.
 */
@Entity
@Table(name = "conversations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_conversation_participants",
                columnNames = {"participant_one_id", "participant_two_id"})
})
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_one_id", nullable = false)
    private User participantOne;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_two_id", nullable = false)
    private User participantTwo;

    /** Opciona veza sa tiketom povodom koga je prepiska zapoceta. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sentAt ASC")
    private List<Message> messages = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Vreme poslednje poruke - koristi se za sortiranje liste prepiski. */
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    protected Conversation() {
    }

    /**
     * Kreira prepisku sa normalizovanim redosledom ucesnika.
     *
     * @throws IllegalArgumentException ako su oba ucesnika ista osoba
     */
    public Conversation(User first, User second, Ticket ticket) {
        Objects.requireNonNull(first, "prvi ucesnik je obavezan");
        Objects.requireNonNull(second, "drugi ucesnik je obavezan");
        if (Objects.equals(first.getId(), second.getId())) {
            throw new IllegalArgumentException("Prepiska mora imati dva razlicita ucesnika");
        }
        if (first.getId() != null && second.getId() != null && first.getId() > second.getId()) {
            this.participantOne = second;
            this.participantTwo = first;
        } else {
            this.participantOne = first;
            this.participantTwo = second;
        }
        this.ticket = ticket;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void addMessage(Message message) {
        messages.add(message);
        message.setConversation(this);
        lastMessageAt = message.getSentAt() != null ? message.getSentAt() : LocalDateTime.now();
    }

    /** Provera da li dati korisnik uopste sme da vidi ovu prepisku. */
    public boolean hasParticipant(Long userId) {
        return Objects.equals(participantOne.getId(), userId)
                || Objects.equals(participantTwo.getId(), userId);
    }

    /** Vraca sagovornika iz ugla prosledjenog korisnika. */
    public User counterpartOf(Long userId) {
        return Objects.equals(participantOne.getId(), userId) ? participantTwo : participantOne;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getParticipantOne() {
        return participantOne;
    }

    public void setParticipantOne(User participantOne) {
        this.participantOne = participantOne;
    }

    public User getParticipantTwo() {
        return participantTwo;
    }

    public void setParticipantTwo(User participantTwo) {
        this.participantTwo = participantTwo;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Conversation other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
