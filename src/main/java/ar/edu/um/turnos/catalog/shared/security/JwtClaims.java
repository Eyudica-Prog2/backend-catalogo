package ar.edu.um.turnos.catalog.shared.security;

/**
 * Names of the claims and the constant values shared by the adapter that issues the end user
 * JWT and the configuration that validates it.
 *
 * <p>Both sides must agree on these names: a token whose {@code authorities} claim is
 * spelled differently would authenticate the user but authorize nothing.</p>
 */
public final class JwtClaims {

    /** Issuer of the tokens of this service. */
    public static final String ISSUER = "backend-catalogo";

    /** Login of the user; the standard {@code sub} claim. */
    public static final String SUBJECT = "sub";

    /** Stable identifier of the account, the one other services receive. */
    public static final String PUBLIC_ID = "publicId";

    /** Roles of the user, a list of strings such as {@code ["ROLE_USER"]}. */
    public static final String AUTHORITIES = "authorities";

    private JwtClaims() {
    }
}
