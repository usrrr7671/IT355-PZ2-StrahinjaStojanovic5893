package rs.ac.metropolitan.it355.helpdesk.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.metropolitan.it355.helpdesk.dto.PriorityRequest;
import rs.ac.metropolitan.it355.helpdesk.dto.PriorityResponse;
import rs.ac.metropolitan.it355.helpdesk.exception.BusinessRuleException;
import rs.ac.metropolitan.it355.helpdesk.exception.DuplicateResourceException;
import rs.ac.metropolitan.it355.helpdesk.exception.ResourceNotFoundException;
import rs.ac.metropolitan.it355.helpdesk.model.Priority;
import rs.ac.metropolitan.it355.helpdesk.repository.PriorityRepository;
import rs.ac.metropolitan.it355.helpdesk.repository.TicketRepository;

import java.util.List;

/**
 * Sifarnik prioriteta sa pripadajucim SLA rokovima.
 */
@Service
public class PriorityService {

    private final PriorityRepository priorityRepository;
    private final TicketRepository ticketRepository;

    public PriorityService(PriorityRepository priorityRepository, TicketRepository ticketRepository) {
        this.priorityRepository = priorityRepository;
        this.ticketRepository = ticketRepository;
    }

    /** Sortirano od najhitnijeg, jer se tako i prikazuje u padajucoj listi. */
    @Transactional(readOnly = true)
    public List<PriorityResponse> list() {
        return priorityRepository.findAllByOrderByLevelDesc().stream()
                .map(PriorityResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PriorityResponse getById(Long id) {
        return PriorityResponse.from(findOrThrow(id));
    }

    @Transactional
    public PriorityResponse create(PriorityRequest request) {
        if (priorityRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("Prioritet '" + request.name() + "' vec postoji");
        }

        Priority priority = new Priority(request.name(), request.level(), request.slaHours());
        return PriorityResponse.from(priorityRepository.save(priority));
    }

    @Transactional
    public PriorityResponse update(Long id, PriorityRequest request) {
        Priority priority = findOrThrow(id);

        if (!priority.getName().equalsIgnoreCase(request.name())
                && priorityRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("Prioritet '" + request.name() + "' vec postoji");
        }

        priority.setName(request.name());
        priority.setLevel(request.level());
        priority.setSlaHours(request.slaHours());
        return PriorityResponse.from(priority);
    }

    /**
     * Prioritet koji je u upotrebi se ne sme obrisati - za razliku od kategorije,
     * on nema zastavicu "aktivan", pa bi brisanje ostavilo tikete bez obaveznog polja.
     */
    @Transactional
    public void delete(Long id) {
        Priority priority = findOrThrow(id);

        if (ticketRepository.existsByPriorityId(id)) {
            throw new BusinessRuleException(
                    "Prioritet '" + priority.getName() + "' se koristi na postojecim tiketima i ne moze se obrisati");
        }

        priorityRepository.delete(priority);
    }

    private Priority findOrThrow(Long id) {
        return priorityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prioritet", id));
    }
}
