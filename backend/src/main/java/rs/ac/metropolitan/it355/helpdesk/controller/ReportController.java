package rs.ac.metropolitan.it355.helpdesk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.ac.metropolitan.it355.helpdesk.dto.DashboardStatsResponse;
import rs.ac.metropolitan.it355.helpdesk.security.UserPrincipal;
import rs.ac.metropolitan.it355.helpdesk.service.ReportService;

/**
 * Zbirni izvestaji. Podaci o celom sistemu nisu za krajnjeg korisnika, pa je cela
 * putanja /api/reports/** otvorena samo za osoblje podrske.
 */
@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
@Tag(name = "Izvestaji", description = "Zbirni pokazatelji za osoblje podrske")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Nadzorna tabla",
            description = """
                    Broj tiketa po statusu i kategoriji, broj nedodeljenih tiketa,
                    broj tiketa sa prekoracenim SLA rokom i velicina sopstvenog radnog reda.
                    """)
    public ResponseEntity<DashboardStatsResponse> dashboard(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(reportService.dashboard(principal));
    }
}
