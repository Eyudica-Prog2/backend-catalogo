package ar.edu.um.turnos.catalog.support;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Mints end user JWTs for tests, signed with the same HS256 secret used by the service.
 *
 * <p>The claims are the ones the service itself issues (issuer {@code backend-catalogo},
 * {@code sub}, {@code publicId} and {@code authorities}), because the decoder validates the
 * issuer as well as the signature: a token without it is not a token of this service.</p>
 */
public final class TestTokens {

    private TestTokens() {
    }

    /**
     * @param secret HS256 secret, at least 32 characters
     * @param login  value of the {@code sub} claim
     * @return a signed token valid for one hour, holding {@code ROLE_USER}
     */
    public static String hs256(String secret, String login) {
        return hs256(secret, login, List.of("ROLE_USER"));
    }

    /**
     * @param secret      HS256 secret, at least 32 characters
     * @param login       value of the {@code sub} claim
     * @param authorities value of the {@code authorities} claim, may be empty to build a
     *                    token that authenticates but authorizes nothing
     * @return a signed token valid for one hour
     */
    public static String hs256(String secret, String login, List<String> authorities) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(login)
                    .issuer("backend-catalogo")
                    .claim("publicId", UUID.randomUUID().toString())
                    .claim("authorities", authorities)
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + 3_600_000L))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (JOSEException ex) {
            throw new IllegalStateException("Could not sign the test token.", ex);
        }
    }
}
