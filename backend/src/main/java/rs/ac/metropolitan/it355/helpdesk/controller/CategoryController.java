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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.ac.metropolitan.it355.helpdesk.dto.CategoryRequest;
import rs.ac.metropolitan.it355.helpdesk.dto.CategoryResponse;
import rs.ac.metropolitan.it355.helpdesk.service.CategoryService;

import java.util.List;

/**
 * Sifarnik kategorija.
 *
 * Citanje je javno jer je lista kategorija potrebna na formi za prijavu problema,
 * dok su sve izmene rezervisane za administratora - i na nivou rute i anotacijom.
 */
@RestController
@RequestMapping("/api/categories")
@Tag(name = "Kategorije", description = "Sifarnik kategorija tiketa (izmene: samo administrator)")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "Lista kategorija",
            description = "Podrazumevano vraca samo aktivne; sa onlyActive=false i one povucene iz upotrebe.")
    public ResponseEntity<List<CategoryResponse>> list(
            @RequestParam(defaultValue = "true") boolean onlyActive) {
        return ResponseEntity.ok(categoryService.list(onlyActive));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Jedna kategorija")
    public ResponseEntity<CategoryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Nova kategorija")
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Izmena kategorije")
    public ResponseEntity<CategoryResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Brisanje kategorije",
            description = """
                    Kategorija koja se koristi na postojecim tiketima se ne brise nego
                    deaktivira, da se ne izgubi podatak o klasifikaciji tih tiketa.
                    """)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
