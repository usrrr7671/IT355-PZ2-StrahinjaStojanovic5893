package rs.ac.metropolitan.it355.helpdesk.dto;

/**
 * Odgovor na uspesnu prijavu ili registraciju.
 *
 * @param token     JWT koji klijent salje u Authorization zaglavlju
 * @param tokenType sema autentifikacije, uvek "Bearer"
 * @param expiresIn vreme trajanja tokena u milisekundama
 * @param user      podaci o prijavljenom nalogu, da klijent ne mora odmah po njih
 */
public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn,
        UserResponse user) {

    public static AuthResponse of(String token, long expiresIn, UserResponse user) {
        return new AuthResponse(token, "Bearer", expiresIn, user);
    }
}
