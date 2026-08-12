package rs.ac.metropolitan.it355.helpdesk.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import rs.ac.metropolitan.it355.helpdesk.exception.ApiError;

import java.io.IOException;

/**
 * Odgovor na zahtev bez (ili sa neispravnim) tokenom.
 *
 * Podrazumevano ponasanje Spring Security-ja je preusmeravanje na login stranicu,
 * sto REST klijentu ne znaci nista - zato se vraca 401 sa JSON telom.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiError error = ApiError.of(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "Niste prijavljeni ili je vas token istekao.",
                request.getRequestURI());

        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
