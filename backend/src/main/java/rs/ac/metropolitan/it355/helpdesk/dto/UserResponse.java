package rs.ac.metropolitan.it355.helpdesk.dto;

import rs.ac.metropolitan.it355.helpdesk.model.Role;
import rs.ac.metropolitan.it355.helpdesk.model.User;

import java.time.LocalDateTime;

/**
 * Prikaz naloga ka klijentu. Namerno ne sadrzi polje sa lozinkom - entitet
 * {@link User} se nikada ne serijalizuje direktno, vec uvek prolazi kroz ovaj DTO.
 */
public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        Role role,
        boolean active,
        LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt());
    }
}
