package rs.ac.metropolitan.it355.helpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.ac.metropolitan.it355.helpdesk.model.Message;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderBySentAtAsc(Long conversationId);

    /**
     * Poruke sa vec ucitanim posiljaocem - bez ovoga bi prikaz prepiske od
     * trideset poruka pokrenuo trideset dodatnih upita za imena posiljalaca.
     */
    @Query("""
            SELECT m FROM Message m
            JOIN FETCH m.sender
            WHERE m.conversation.id = :conversationId
            ORDER BY m.sentAt ASC
            """)
    List<Message> findByConversationWithSender(@Param("conversationId") Long conversationId);

    /** Broj neprocitanih poruka u jednoj prepisci iz ugla prijavljenog korisnika. */
    @Query("""
            SELECT COUNT(m) FROM Message m
            WHERE m.conversation.id = :conversationId
              AND m.sender.id <> :userId
              AND m.readAt IS NULL
            """)
    long countUnreadInConversation(@Param("conversationId") Long conversationId,
                                   @Param("userId") Long userId);

    /** Ukupan broj neprocitanih poruka korisnika - prikazuje se kao znacka u meniju. */
    @Query("""
            SELECT COUNT(m) FROM Message m
            WHERE (m.conversation.participantOne.id = :userId OR m.conversation.participantTwo.id = :userId)
              AND m.sender.id <> :userId
              AND m.readAt IS NULL
            """)
    long countUnreadForUser(@Param("userId") Long userId);

    /**
     * Masovno obelezavanje tudjih poruka kao procitanih prilikom otvaranja prepiske.
     * Jedan UPDATE upit umesto ucitavanja i cuvanja svake poruke ponaosob.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Message m SET m.readAt = :readAt
            WHERE m.conversation.id = :conversationId
              AND m.sender.id <> :userId
              AND m.readAt IS NULL
            """)
    int markConversationAsRead(@Param("conversationId") Long conversationId,
                               @Param("userId") Long userId,
                               @Param("readAt") LocalDateTime readAt);
}
