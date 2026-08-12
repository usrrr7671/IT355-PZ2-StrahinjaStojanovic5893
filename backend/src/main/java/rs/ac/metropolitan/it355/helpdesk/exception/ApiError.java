package rs.ac.metropolitan.it355.helpdesk.exception;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Jedinstven oblik odgovora za sve greske, da bi frontend imao jedno mesto
 * na kome cita poruku bez obzira na to koja je greska u pitanju.
 *
 * @param fieldErrors popunjeno samo kod greske validacije (naziv polja -> poruka)
 */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(LocalDateTime.now(), status, error, message, path, null);
    }

    public static ApiError validation(int status, String message, String path, Map<String, String> fieldErrors) {
        return new ApiError(LocalDateTime.now(), status, "Bad Request", message, path, fieldErrors);
    }
}
