package rs.ac.metropolitan.it355.helpdesk.exception;

/** Zapis sa datom jedinstvenom vrednoscu vec postoji - mapira se na HTTP 409. */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
