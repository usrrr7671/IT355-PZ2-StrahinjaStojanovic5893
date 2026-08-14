package rs.ac.metropolitan.it355.helpdesk.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.metropolitan.it355.helpdesk.dto.CategoryRequest;
import rs.ac.metropolitan.it355.helpdesk.dto.CategoryResponse;
import rs.ac.metropolitan.it355.helpdesk.exception.BusinessRuleException;
import rs.ac.metropolitan.it355.helpdesk.exception.DuplicateResourceException;
import rs.ac.metropolitan.it355.helpdesk.exception.ResourceNotFoundException;
import rs.ac.metropolitan.it355.helpdesk.model.Category;
import rs.ac.metropolitan.it355.helpdesk.repository.CategoryRepository;
import rs.ac.metropolitan.it355.helpdesk.repository.TicketRepository;

import java.util.List;

/**
 * Sifarnik kategorija. Citanje je javno (potrebno je i na formi za registraciju
 * problema pre prijave), a izmene su rezervisane za administratora.
 */
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TicketRepository ticketRepository;

    public CategoryService(CategoryRepository categoryRepository, TicketRepository ticketRepository) {
        this.categoryRepository = categoryRepository;
        this.ticketRepository = ticketRepository;
    }

    /** @param onlyActive kada je true vraca samo kategorije koje se jos nude na formi */
    @Transactional(readOnly = true)
    public List<CategoryResponse> list(boolean onlyActive) {
        List<Category> categories = onlyActive
                ? categoryRepository.findByActiveTrueOrderByNameAsc()
                : categoryRepository.findAll();
        return categories.stream().map(CategoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        return CategoryResponse.from(findOrThrow(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("Kategorija '" + request.name() + "' vec postoji");
        }

        Category category = new Category(request.name(), request.description());
        if (request.active() != null) {
            category.setActive(request.active());
        }
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findOrThrow(id);

        if (!category.getName().equalsIgnoreCase(request.name())
                && categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("Kategorija '" + request.name() + "' vec postoji");
        }

        category.setName(request.name());
        category.setDescription(request.description());
        if (request.active() != null) {
            category.setActive(request.active());
        }
        return CategoryResponse.from(category);
    }

    /**
     * Kategorija koja se vec koristi na nekom tiketu se ne brise, nego deaktivira -
     * brisanjem bi se izgubio podatak o tome kako je tiket bio klasifikovan.
     */
    @Transactional
    public void delete(Long id) {
        Category category = findOrThrow(id);

        if (ticketRepository.existsByCategoryId(id)) {
            if (!category.isActive()) {
                throw new BusinessRuleException(
                        "Kategorija se koristi na postojecim tiketima i vec je deaktivirana");
            }
            category.setActive(false);
            return;
        }

        categoryRepository.delete(category);
    }

    private Category findOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kategorija", id));
    }
}
