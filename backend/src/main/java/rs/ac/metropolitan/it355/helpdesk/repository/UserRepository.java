package rs.ac.metropolitan.it355.helpdesk.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.ac.metropolitan.it355.helpdesk.model.Role;
import rs.ac.metropolitan.it355.helpdesk.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** Koristi ga UserDetailsService prilikom prijave. */
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    Page<User> findByRole(Role role, Pageable pageable);

    /**
     * Pretraga naloga po korisnickom imenu, imenu i prezimenu ili e-adresi.
     * Primer prilagodjene JPQL upitne metode sa @Query anotacijom.
     */
    @Query("""
            SELECT u FROM User u
            WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :term, '%'))
            """)
    Page<User> search(@Param("term") String term, Pageable pageable);

    /**
     * Agent sa najmanje otvorenih tiketa - koristi se za automatsku dodelu tiketa.
     * Vraca listu sortiranu rastuce po broju aktivnih tiketa, pa se uzima prvi element.
     */
    @Query("""
            SELECT u FROM User u
            LEFT JOIN Ticket t ON t.assignee = u AND t.status <> rs.ac.metropolitan.it355.helpdesk.model.TicketStatus.CLOSED
            WHERE u.role = rs.ac.metropolitan.it355.helpdesk.model.Role.AGENT AND u.active = true
            GROUP BY u
            ORDER BY COUNT(t) ASC
            """)
    List<User> findAgentsOrderedByWorkload(Pageable pageable);
}
