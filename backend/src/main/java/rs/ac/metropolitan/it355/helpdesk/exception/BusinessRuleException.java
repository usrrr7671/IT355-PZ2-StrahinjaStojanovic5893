package rs.ac.metropolitan.it355.helpdesk.exception;

/**
 * Zahtev je sintaksno ispravan, ali krsi poslovno pravilo
 * (npr. nedozvoljen prelaz statusa tiketa) - mapira se na HTTP 400.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
