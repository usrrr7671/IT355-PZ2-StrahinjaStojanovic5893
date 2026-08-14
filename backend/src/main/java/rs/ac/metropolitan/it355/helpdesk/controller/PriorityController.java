package rs.ac.metropolitan.it355.helpdesk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.ac.metropolitan.it355.helpdesk.dto.PriorityRequest;
import rs.ac.metropolitan.it355.helpdesk.dto.PriorityResponse;
import rs.ac.metropolitan.it355.helpdesk.service.PriorityService;

import java.util.List;

/**
 * Sifarnik prioriteta sa SLA rokovima.
 */
@RestController
@RequestMapping("/api/priorities")
@Tag(name = "Prioriteti", description = "Sifarnik prioriteta i SLA rokova (izmene: samo administrator)")
public class PriorityController {

    private final PriorityService priorityService;

    public PriorityController(PriorityService priorityService) {
        this.priorityService = priorityService;
    }

    @GetMapping
    @Operation(summary = "Lista prioriteta", description = "Sortirano od najhitnijeg ka najmanje hitnom.")
    public ResponseEntity<List<PriorityResponse>> list() {
        return ResponseEntity.ok(priorityService.list());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Jedan prioritet")
    public ResponseEntity<PriorityResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(priorityService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Novi prioritet")
    public ResponseEntity<PriorityResponse> create(@Valid @RequestBody PriorityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(priorityService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Izmena prioriteta",
            description = "Promena SLA sati odmah utice na racunanje roka svih tiketa tog prioriteta.")
    public ResponseEntity<PriorityResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody PriorityRequest request) {
        return ResponseEntity.ok(priorityService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Brisanje prioriteta",
            description = "Odbija se ako se prioritet koristi na nekom tiketu, jer je polje obavezno.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        priorityService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
