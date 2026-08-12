package rs.ac.metropolitan.it355.helpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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
}
