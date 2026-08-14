package rs.ac.metropolitan.it355.helpdesk.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import rs.ac.metropolitan.it355.helpdesk.dto.ChangeStatusRequest;
import rs.ac.metropolitan.it355.helpdesk.dto.CreateCommentRequest;
import rs.ac.metropolitan.it355.helpdesk.dto.CreateTicketRequest;
import rs.ac.metropolitan.it355.helpdesk.dto.LoginRequest;
import rs.ac.metropolitan.it355.helpdesk.model.TicketStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integracioni testovi tiketa, sa tezistem na kontroli pristupa.
 *
 * Zahtevi se salju sa stvarnim JWT tokenom dobijenim prijavom, pa se proverava ceo
 * put: filter koji cita token, autorizacija na nivou rute, pa provera vlasnistva
 * nad zapisom u servisu. Time se dokazuje da RBAC radi u sklopu, a ne samo u
 * izolovanim jedinicnim testovima.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Integracioni testovi - tiketi i RBAC")
class TicketApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String peraToken;
    private String mikaToken;
    private String agentToken;
    private String adminToken;

    private Long kategorijaId;
    private Long prioritetId;

    @BeforeEach
    void setUp() throws Exception {
        peraToken = prijaviSe("pera");
        mikaToken = prijaviSe("mika");
        agentToken = prijaviSe("agent1");
        adminToken = prijaviSe("admin");

        kategorijaId = prviId("/api/categories");
        prioritetId = prviId("/api/priorities");
    }

    @Test
    @DisplayName("korisnik prijavljuje tiket koji krece kao NEW i bez agenta")
    void korisnikPrijavljujeTiket() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + peraToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateTicketRequest(
                                "Ne radi mrezna stampa",
                                "Dokumenti ostaju u redu za stampu i nikada se ne odstampaju.",
                                kategorijaId, prioritetId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.assignee").doesNotExist())
                .andExpect(jsonPath("$.reporter.username").value("pera"));
    }

    @Test
    @DisplayName("korisnik ne moze da otvori tudji tiket")
    void tudjiTiketVracaZabranu() throws Exception {
        Long tudjiTiket = kreirajTiket(mikaToken, "Problem sa monitorom kod korisnika mika");

        mockMvc.perform(get("/api/tickets/" + tudjiTiket)
                        .header("Authorization", "Bearer " + peraToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("agent moze da otvori tiket koji nije njegov")
    void agentVidiSvakiTiket() throws Exception {
        Long tiket = kreirajTiket(peraToken, "Tiket koji agent treba da vidi");

        mockMvc.perform(get("/api/tickets/" + tiket)
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reporter.username").value("pera"));
    }

    @Test
    @DisplayName("korisnik ne sme da menja status tiketa")
    void korisnikNeMenjaStatus() throws Exception {
        Long tiket = kreirajTiket(peraToken, "Tiket ciji status korisnik pokusava da promeni");

        mockMvc.perform(patch("/api/tickets/" + tiket + "/status")
                        .header("Authorization", "Bearer " + peraToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ChangeStatusRequest(TicketStatus.IN_PROGRESS, "pokusaj"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("agent preuzima tiket i vodi ga kroz zivotni ciklus")
    void agentVodiTiketKrozCiklus() throws Exception {
        Long tiket = kreirajTiket(peraToken, "Tiket koji agent obradjuje do kraja");

        // Dodela prebacuje tiket iz NEW u OPEN.
        mockMvc.perform(patch("/api/tickets/" + tiket + "/assign")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.assignee").exists());

        mockMvc.perform(patch("/api/tickets/" + tiket + "/status")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ChangeStatusRequest(TicketStatus.IN_PROGRESS, "Radim na tome"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(patch("/api/tickets/" + tiket + "/status")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ChangeStatusRequest(TicketStatus.RESOLVED, "Reseno"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                // Svaka promena je ostavila trag: NEW, OPEN, IN_PROGRESS, RESOLVED.
                .andExpect(jsonPath("$.history.length()").value(4));
    }

    @Test
    @DisplayName("preskakanje koraka u zivotnom ciklusu vraca 400")
    void nedozvoljenPrelazVracaGresku() throws Exception {
        Long tiket = kreirajTiket(peraToken, "Tiket na kome se proverava prelaz statusa");

        mockMvc.perform(patch("/api/tickets/" + tiket + "/status")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ChangeStatusRequest(TicketStatus.RESOLVED, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("nije dozvoljen")));
    }

    @Test
    @DisplayName("interna beleska se ne prikazuje korisniku")
    void internaBeleskaOstajeSkrivena() throws Exception {
        Long tiket = kreirajTiket(peraToken, "Tiket sa internom beleskom podrske");

        mockMvc.perform(post("/api/tickets/" + tiket + "/comments")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateCommentRequest("Tajna beleska podrske", true))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/tickets/" + tiket + "/comments")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateCommentRequest("Javni odgovor korisniku", false))))
                .andExpect(status().isCreated());

        // Agent vidi oba komentara.
        mockMvc.perform(get("/api/tickets/" + tiket + "/comments")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // Korisnik vidi samo javni, a tekst interne beleske se nigde ne pojavljuje.
        String odgovor = mockMvc.perform(get("/api/tickets/" + tiket + "/comments")
                        .header("Authorization", "Bearer " + peraToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andReturn().getResponse().getContentAsString();

        assertThat(odgovor).doesNotContain("Tajna beleska podrske");
    }

    @Test
    @DisplayName("korisnik ne moze da napise internu belesku")
    void korisnikNePiseInternuBelesku() throws Exception {
        Long tiket = kreirajTiket(peraToken, "Tiket na kome korisnik pokusava internu belesku");

        mockMvc.perform(post("/api/tickets/" + tiket + "/comments")
                        .header("Authorization", "Bearer " + peraToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateCommentRequest("Pokusaj skrivanja", true))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("agent ne sme u administraciju naloga, administrator sme")
    void administracijaJeSamoZaAdmina() throws Exception {
        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("korisnik ne sme na izvestaje, agent sme")
    void izvestajiSuZaOsoblje() throws Exception {
        mockMvc.perform(get("/api/reports/dashboard").header("Authorization", "Bearer " + peraToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/reports/dashboard").header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketsByStatus").exists());
    }

    @Test
    @DisplayName("korisnik u listi tiketa vidi iskljucivo svoje")
    void listaSadrziSamoSopstveneTikete() throws Exception {
        kreirajTiket(peraToken, "Jedan tiket korisnika pera");
        kreirajTiket(mikaToken, "Jedan tiket korisnika mika");

        String odgovor = mockMvc.perform(get("/api/tickets")
                        .header("Authorization", "Bearer " + peraToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode sadrzaj = objectMapper.readTree(odgovor).get("content");
        assertThat(sadrzaj).isNotEmpty();
        sadrzaj.forEach(tiket ->
                assertThat(tiket.get("reporter").get("username").asText()).isEqualTo("pera"));
    }

    @Test
    @DisplayName("neispravni podaci pri prijavi tiketa vracaju 400 sa greskama po polju")
    void validacijaTiketa() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + peraToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateTicketRequest("kr", "kratko", null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.categoryId").isNotEmpty());
    }

    // ------------------------------------------------------------------
    // Pomocne metode
    // ------------------------------------------------------------------

    private String prijaviSe(String korisnickoIme) throws Exception {
        String odgovor = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest(korisnickoIme, "lozinka123"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(odgovor).get("token").asText();
    }

    private Long kreirajTiket(String token, String naslov) throws Exception {
        String odgovor = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateTicketRequest(naslov,
                                "Detaljan opis problema za potrebe integracionog testa.",
                                kategorijaId, prioritetId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(odgovor).get("id").asLong();
    }

    private Long prviId(String putanja) throws Exception {
        String odgovor = mockMvc.perform(get(putanja))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(odgovor).get(0).get("id").asLong();
    }

    private String json(Object vrednost) throws Exception {
        return objectMapper.writeValueAsString(vrednost);
    }
}
