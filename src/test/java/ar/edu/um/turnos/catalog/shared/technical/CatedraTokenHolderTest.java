package ar.edu.um.turnos.catalog.shared.technical;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.junit.jupiter.api.Test;

/**
 * The technical token lives only in memory, so these tests pin down its reuse and its
 * expiration handling: a stale token must never be sent to the catedra.
 */
class CatedraTokenHolderTest {

    private static final String SECRET = "a-sufficiently-long-secret-for-hs256-signing";

    private final CatedraTokenHolder holder = new CatedraTokenHolder();

    @Test
    void startsEmpty() {
        assertThat(holder.currentToken()).isEmpty();
        assertThat(holder.hasToken()).isFalse();
    }

    @Test
    void reusesAStoredToken() {
        holder.store("some-token");

        assertThat(holder.currentToken()).contains("some-token");
        assertThat(holder.hasToken()).isTrue();
    }

    @Test
    void clearsOnBlankOrExplicitClear() {
        holder.store("some-token");
        holder.clear();
        assertThat(holder.currentToken()).isEmpty();

        holder.store("another-token");
        holder.store("   ");
        assertThat(holder.currentToken()).isEmpty();
    }

    @Test
    void ignoresATokenThatIsAboutToExpire() {
        String token = signedToken(System.currentTimeMillis() + 30_000L);

        holder.store(token);

        // The token is still valid but too close to its expiration date.
        assertThat(holder.currentToken()).isEmpty();
    }

    @Test
    void keepsATokenThatIsFarFromExpiring() {
        String token = signedToken(System.currentTimeMillis() + 3_600_000L);

        holder.store(token);

        assertThat(holder.currentToken()).contains(token);
    }

    @Test
    void acceptsANonJwtValueAndLetsTheApiAnswer() {
        holder.store("opaque-value");

        assertThat(holder.currentToken()).contains("opaque-value");
    }

    private static String signedToken(long expiresAtMillis) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject("technical-login")
                    .issueTime(new Date(System.currentTimeMillis() - 60_000L))
                    .expirationTime(new Date(expiresAtMillis))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (JOSEException ex) {
            throw new IllegalStateException("Could not sign the test token.", ex);
        }
    }
}
