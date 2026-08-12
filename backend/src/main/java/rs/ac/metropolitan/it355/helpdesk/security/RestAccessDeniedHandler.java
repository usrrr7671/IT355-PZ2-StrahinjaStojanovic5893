package rs.ac.metropolitan.it355.helpdesk.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import rs.ac.metropolitan.it355.helpdesk.exception.ApiError;

import java.io.IOException;

/**
 * Korisnik jeste prijavljen, ali njegova uloga nije dovoljna za trazenu akciju - HTTP 403.
 * Razlika u odnosu na 401 je bitna: 401 znaci "prijavi se", 403 znaci "nemas pravo".
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiError error = ApiError.of(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                "Nemate ovlascenje za ovu akciju.",
                request.getRequestURI());

        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
