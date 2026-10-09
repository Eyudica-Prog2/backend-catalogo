package ar.edu.unlp.turnos.catalog.shared.technical;

import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

/**
 * In-memory holder of the technical token issued by the catedra.
 *
 * <p>The token is deliberately kept out of the database and out of every log line: it is a
 * one year credential of the project and can be obtained again with the technical login.
 * The value never leaves this package, so it cannot reach KMP by accident.</p>
 */
@Component
public class CatedraTokenHolder {

    /**
     * Consider a token expired this long before its real expiration date, to avoid using a
     * token that expires while the request is in flight.
     */
    private static final Duration EXPIRY_SKEW = Duration.ofSeconds(60);

    private final AtomicReference<TokenState> state = new AtomicReference<>();

    /**
     * @return the current token, empty when there is none or it is about to expire
     */
    public Optional<String> currentToken() {
        TokenState current = state.get();
        if (current == null) {
            return Optional.empty();
        }
        if (current.expiresAt() != null && !current.expiresAt().minus(EXPIRY_SKEW).isAfter(Instant.now())) {
            state.compareAndSet(current, null);
            return Optional.empty();
        }
        return Optional.of(current.token());
    }

    /**
     * @param token token returned by the catedra; null or blank clears the holder
     */
    public void store(String token) {
        if (token == null || token.isBlank()) {
            clear();
            return;
        }
        state.set(new TokenState(token.trim(), readExpiration(token.trim())));
    }

    /**
     * Forgets the current token, for example after an HTTP 401.
     */
    public void clear() {
        state.set(null);
    }

    /**
     * @return true when a token is currently usable
     */
    public boolean hasToken() {
        return currentToken().isPresent();
    }

    private static Instant readExpiration(String token) {
        try {
            SignedJWT signed = SignedJWT.parse(token);
            java.util.Date expiration = signed.getJWTClaimsSet().getExpirationTime();
            return expiration == null ? null : expiration.toInstant();
        } catch (ParseException ex) {
            // Not a parseable JWT: trust the catedra and let the API answer with a 401 if wrong.
            return null;
        }
    }

    private record TokenState(String token, Instant expiresAt) {
    }
}
