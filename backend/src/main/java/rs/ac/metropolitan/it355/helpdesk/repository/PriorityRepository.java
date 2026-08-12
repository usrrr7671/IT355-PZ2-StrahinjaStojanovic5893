package rs.ac.metropolitan.it355.helpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.ac.metropolitan.it355.helpdesk.model.Priority;

import java.util.List;
import java.util.Optional;

@Repository
public interface PriorityRepository extends JpaRepository<Priority, Long> {

    Optional<Priority> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    /** Sortirano od najhitnijeg ka najmanje hitnom. */
    List<Priority> findAllByOrderByLevelDesc();
}
