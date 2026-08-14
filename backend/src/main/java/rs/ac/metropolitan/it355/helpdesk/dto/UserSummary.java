package rs.ac.metropolitan.it355.helpdesk.dto;

import rs.ac.metropolitan.it355.helpdesk.model.Role;
import rs.ac.metropolitan.it355.helpdesk.model.User;

/**
 * Skraceni prikaz naloga koji se ugradjuje u druge odgovore (autor komentara,
 * prijavilac tiketa, sagovornik u prepisci). Nosi samo ono sto je potrebno za
 * prikaz imena, bez e-adrese i ostalih podataka koji tu nemaju sta da traze.
 */
public record UserSummary(Long id, String username, String fullName, Role role) {

    public static UserSummary from(User user) {
        return user == null ? null
                : new UserSummary(user.getId(), user.getUsername(), user.getFullName(), user.getRole());
    }
}
