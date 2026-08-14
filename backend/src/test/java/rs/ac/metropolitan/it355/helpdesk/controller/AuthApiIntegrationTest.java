package rs.ac.metropolitan.it355.helpdesk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import rs.ac.metropolitan.it355.helpdesk.dto.LoginRequest;
import rs.ac.metropolitan.it355.helpdesk.dto.RegisterRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integracioni testovi autentifikacije.
 *
 * Za razliku od jedinicnih testova, ovde se podize ceo Spring kontekst sa bazom u
 * memoriji i zahtevi prolaze kroz stvarni lanac bezbednosnih filtera - upravo zato
 * se ovim testovima moze dokazati da neprijavljen zahtev zaista dobija 401.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Integracioni testovi - autentifikacija")
class AuthApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @DisplayName("prijava ispravnim kredencijalima vraca JWT token")
    void prijavaVracaToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "lozinka123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.username").value("admin"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                // Kljucna provera: hes lozinke ne sme da se nadje u odgovoru.
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    @DisplayName("prijava pogresnom lozinkom vraca 401")
    void pogresnaLozinka() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "pogresna"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("zasticena ruta bez tokena vraca 401")
    void bezTokena() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("zasticena ruta sa izmisljenim tokenom vraca 401")
    void neispravanToken() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer ovo.nije.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("registracija kreira nalog sa ulogom USER")
    void registracija() throws Exception {
        RegisterRequest zahtev = new RegisterRequest(
                "novikorisnik", "novi@example.com", "Novi Korisnik", "lozinka123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zahtev)))
                .andExpect(status().isCreated())
                // Uloga se ne prima od klijenta - svaka registracija daje USER.
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("registracija sa zauzetim korisnickim imenom vraca 409")
    void zauzetoKorisnickoIme() throws Exception {
        RegisterRequest zahtev = new RegisterRequest(
                "admin", "drugi@example.com", "Neko Drugi", "lozinka123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zahtev)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("neispravni podaci vracaju 400 sa greskom po polju")
    void validacija() throws Exception {
        RegisterRequest zahtev = new RegisterRequest("ab", "nijemail", "", "123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zahtev)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.username").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.email").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.password").isNotEmpty());
    }
}
