package rs.ac.metropolitan.it355.helpdesk.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.ac.metropolitan.it355.helpdesk.model.Ticket;
import rs.ac.metropolitan.it355.helpdesk.model.TicketStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Page<Ticket> findByReporterId(Long reporterId, Pageable pageable);

    Page<Ticket> findByAssigneeId(Long assigneeId, Pageable pageable);

    long countByStatus(TicketStatus status);

    long countByAssigneeIdAndStatusNot(Long assigneeId, TicketStatus status);

    /** Ucitava tiket zajedno sa vezanim entitetima da bi se izbegao N+1 problem. */
    @Query("""
            SELECT t FROM Ticket t
            LEFT JOIN FETCH t.reporter
            LEFT JOIN FETCH t.assignee
            LEFT JOIN FETCH t.category
            LEFT JOIN FETCH t.priority
            WHERE t.id = :id
            """)
    Optional<Ticket> findByIdWithRelations(@Param("id") Long id);

    /**
     * Jedinstvena pretraga tiketa sa opcionim filterima. Kada je parametar null,
     * odgovarajuci uslov se preskace, pa jedna metoda pokriva sve kombinacije filtera.
     */
    @Query("""
            SELECT t FROM Ticket t
            WHERE (:status     IS NULL OR t.status = :status)
              AND (:categoryId IS NULL OR t.category.id = :categoryId)
              AND (:priorityId IS NULL OR t.priority.id = :priorityId)
              AND (:assigneeId IS NULL OR t.assignee.id = :assigneeId)
              AND (:reporterId IS NULL OR t.reporter.id = :reporterId)
              AND (:term       IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :term, '%'))
                                       OR LOWER(t.description) LIKE LOWER(CONCAT('%', :term, '%')))
            """)
    Page<Ticket> search(@Param("status") TicketStatus status,
                        @Param("categoryId") Long categoryId,
                        @Param("priorityId") Long priorityId,
                        @Param("assigneeId") Long assigneeId,
                        @Param("reporterId") Long reporterId,
                        @Param("term") String term,
                        Pageable pageable);

    /** Broj tiketa po statusu - podatak za administratorsku statistiku. */
    @Query("SELECT t.status, COUNT(t) FROM Ticket t GROUP BY t.status")
    List<Object[]> countGroupedByStatus();

    /** Broj tiketa po kategoriji, sortirano od najopterecenije kategorije. */
    @Query("SELECT t.category.name, COUNT(t) FROM Ticket t GROUP BY t.category.name ORDER BY COUNT(t) DESC")
    List<Object[]> countGroupedByCategory();

    boolean existsByCategoryId(Long categoryId);

    boolean existsByPriorityId(Long priorityId);
}
