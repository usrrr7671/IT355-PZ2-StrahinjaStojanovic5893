package rs.ac.metropolitan.it355.helpdesk.model;

/**
 * Uloge u sistemu. Spring Security ocekuje prefiks "ROLE_" kod provere
 * preko hasRole(...), pa se on dodaje prilikom kreiranja GrantedAuthority objekta.
 */
public enum Role {

    /** Krajnji korisnik - prijavljuje tikete i dopisuje se sa agentom. */
    USER,

    /** Agent podrske - preuzima tikete, menja status i pise interne beleske. */
    AGENT,

    /** Administrator - sve sto i agent, plus upravljanje korisnicima i sifarnicima. */
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
