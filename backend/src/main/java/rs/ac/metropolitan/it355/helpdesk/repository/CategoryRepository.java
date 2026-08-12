package rs.ac.metropolitan.it355.helpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.ac.metropolitan.it355.helpdesk.model.Category;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    /** Kategorije ponudjene korisniku prilikom kreiranja tiketa. */
    List<Category> findByActiveTrueOrderByNameAsc();
}
