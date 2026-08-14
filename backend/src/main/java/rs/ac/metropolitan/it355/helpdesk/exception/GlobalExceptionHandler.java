package rs.ac.metropolitan.it355.helpdesk.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Jedno mesto na kome se izuzeci prevode u HTTP odgovore.
 *
 * Zahvaljujuci ovome servisi mogu da bacaju izuzetke koji opisuju poslovni problem,
 * a da nigde u njima ne stoji HTTP status - servisni sloj ne mora da zna da postoji web.
 *
 * Klasa nasledjuje {@link ResponseEntityExceptionHandler} da bi standardni izuzeci
 * Spring MVC-a zadrzali svoj tacan status kod: bez toga bi nepostojeca putanja
 * (NoResourceFoundException) ili pogresna HTTP metoda upali u opsti Exception handler
 * i klijentu bi se, umesto 404 i 405, vracao 500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ------------------------------------------------------------------
    // Nasi poslovni izuzeci
    // ------------------------------------------------------------------

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
    }

    /**
     * Izuzetak koji baci @PreAuthorize ili rucna provera vlasnistva nad zapisom.
     * Poruka je namerno uopstena da ne otkriva da li trazeni zapis uopste postoji.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Forbidden", "Nemate ovlascenje za ovu akciju.", request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", "Pogresno korisnicko ime ili lozinka.", request);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiError> handleDisabled(DisabledException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Forbidden",
                "Vas nalog je deaktiviran. Obratite se administratoru.", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
    }

    /**
     * Prekrsen uslov integriteta u bazi - najcesce jedinstveni indeks ili strani kljuc.
     *
     * Servisi ovakve slucajeve proveravaju unapred, pa je ovo zastita za dva preostala
     * scenarija: dva istovremena zahteva koja prodju istu proveru, i propust u kodu.
     * Bez ovog handlera takva greska bi klijentu stigla kao neprozirna 500.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex,
                                                        HttpServletRequest request) {
        log.warn("Prekrsen uslov integriteta na putanji {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "Conflict",
                "Podatak se ne moze sacuvati jer bi narusio povezanost sa drugim zapisima.", request);
    }

    /**
     * Poslednja linija odbrane. Detalji se upisuju u log, a klijentu se salje uopstena
     * poruka - stack trace u odgovoru bi otkrio unutrasnju strukturu aplikacije.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Neocekivana greska na putanji {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "Doslo je do neocekivane greske na serveru.", request);
    }

    // ------------------------------------------------------------------
    // Prilagodjavanje odgovora koje generise Spring MVC
    // ------------------------------------------------------------------

    /** Greske Bean Validation anotacija (@NotBlank, @Size, ...) - vraca mapu polje -> poruka. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                 HttpHeaders headers,
                                                                 HttpStatusCode status,
                                                                 WebRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

        ApiError error = ApiError.validation(
                HttpStatus.BAD_REQUEST.value(),
                "Podaci u zahtevu nisu ispravni.",
                path(request),
                fieldErrors);

        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Svi ostali standardni izuzeci Spring MVC-a (404, 405, 415, neispravan JSON...)
     * prolaze kroz ovu metodu, pa se ovde njihovo telo zamenjuje nasim ApiError oblikom -
     * time klijent za svaku gresku, bez izuzetka, dobija isti oblik odgovora.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
                                                            @Nullable Object body,
                                                            HttpHeaders headers,
                                                            HttpStatusCode statusCode,
                                                            WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        ApiError error = ApiError.of(status.value(), status.getReasonPhrase(),
                messageFor(status, ex), path(request));

        return ResponseEntity.status(status).headers(headers).body(error);
    }

    private String messageFor(HttpStatus status, Exception ex) {
        return switch (status) {
            case NOT_FOUND -> "Trazeni resurs ne postoji.";
            case METHOD_NOT_ALLOWED -> "HTTP metoda nije dozvoljena na ovoj putanji.";
            case UNSUPPORTED_MEDIA_TYPE -> "Nepodrzan format podataka - koristite application/json.";
            case BAD_REQUEST -> "Zahtev nije ispravan: " + ex.getMessage();
            default -> ex.getMessage();
        };
    }

    private String path(WebRequest request) {
        return request instanceof ServletWebRequest servletRequest
                ? servletRequest.getRequest().getRequestURI()
                : request.getDescription(false);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String error, String message,
                                          HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(ApiError.of(status.value(), error, message, request.getRequestURI()));
    }
}
