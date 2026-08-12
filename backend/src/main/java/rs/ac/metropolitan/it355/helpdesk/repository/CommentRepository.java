package rs.ac.metropolitan.it355.helpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.ac.metropolitan.it355.helpdesk.model.Comment;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    /** Komentari vidljivi korisniku koji nije osoblje podrske - bez internih beleski. */
    List<Comment> findByTicketIdAndInternalFalseOrderByCreatedAtAsc(Long ticketId);

    long countByTicketId(Long ticketId);
}
