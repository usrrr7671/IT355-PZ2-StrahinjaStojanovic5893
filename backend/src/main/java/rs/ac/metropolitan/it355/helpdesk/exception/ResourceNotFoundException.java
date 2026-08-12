package rs.ac.metropolitan.it355.helpdesk.exception;

/** Trazeni zapis ne postoji - mapira se na HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " sa id vrednoscu " + id + " nije pronadjen(a)");
    }
}
