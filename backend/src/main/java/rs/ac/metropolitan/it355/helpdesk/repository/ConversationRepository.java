package rs.ac.metropolitan.it355.helpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.ac.metropolitan.it355.helpdesk.model.Conversation;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /**
     * Prepiske u kojima korisnik ucestvuje, najsvezija prva.
     * NULLS LAST znaci da tek zapocete prepiske bez ijedne poruke idu na dno liste.
     */
    @Query("""
            SELECT c FROM Conversation c
            LEFT JOIN FETCH c.participantOne
            LEFT JOIN FETCH c.participantTwo
            WHERE c.participantOne.id = :userId OR c.participantTwo.id = :userId
            ORDER BY c.lastMessageAt DESC NULLS LAST
            """)
    List<Conversation> findAllForUser(@Param("userId") Long userId);

    /**
     * Trazi postojecu prepisku izmedju dva korisnika bez obzira na redosled ucesnika.
     * Sprecava da se za isti par otvori vise prepiski.
     */
    @Query("""
            SELECT c FROM Conversation c
            WHERE (c.participantOne.id = :firstId  AND c.participantTwo.id = :secondId)
               OR (c.participantOne.id = :secondId AND c.participantTwo.id = :firstId)
            """)
    Optional<Conversation> findBetween(@Param("firstId") Long firstId, @Param("secondId") Long secondId);

    /**
     * Ucitava prepisku sa vec inicijalizovanim ucesnicima i tiketom.
     *
     * JOIN FETCH ovde nije samo optimizacija: otvaranje prepiske usput pokrece
     * masovni UPDATE neprocitanih poruka, koji cisti persistence context i time
     * odvaja ucitane entitete. Lazy proxy bi tada pukao, dok vec ucitane vrednosti
     * i na odvojenom objektu ostaju citljive.
     */
    @Query("""
            SELECT c FROM Conversation c
            JOIN FETCH c.participantOne
            JOIN FETCH c.participantTwo
            LEFT JOIN FETCH c.ticket
            WHERE c.id = :id
            """)
    Optional<Conversation> findByIdWithParticipants(@Param("id") Long id);

    /**
     * Odvezuje prepiske od tiketa koji se brise.
     *
     * Prepiska nije deo tiketa nego pripada sagovornicima, pa ne sme da nestane
     * zajedno sa njim - gubi samo vezu sa tiketom povodom koga je zapoceta.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Conversation c SET c.ticket = null WHERE c.ticket.id = :ticketId")
    int detachFromTicket(@Param("ticketId") Long ticketId);
}
