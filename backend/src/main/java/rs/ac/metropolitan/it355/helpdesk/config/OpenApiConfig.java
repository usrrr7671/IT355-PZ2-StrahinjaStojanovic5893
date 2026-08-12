package rs.ac.metropolitan.it355.helpdesk.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Konfiguracija Swagger/OpenAPI dokumentacije.
 *
 * Registrovanjem "bearerAuth" seme Swagger UI dobija dugme "Authorize", pa se
 * zasticeni endpointi mogu isprobati direktno iz pretrazivaca - dovoljno je
 * nalepiti token dobijen sa /api/auth/login.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI helpDeskOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Help Desk API")
                        .version("1.0.0")
                        .description("""
                                REST API sistema za prijavu i resavanje tiketa tehnicke podrske.

                                Uloge: USER (prijavljuje tikete), AGENT (resava tikete),
                                ADMIN (upravlja nalozima i sifarnicima).

                                IT355 - Veb sistemi 2, drugi projektni zadatak.
                                """)
                        .contact(new Contact()
                                .name("Strahinja Stojanovic, 5893")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Unesite JWT token dobijen na /api/auth/login")));
    }
}
