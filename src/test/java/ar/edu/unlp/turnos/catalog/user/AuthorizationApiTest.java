package ar.edu.unlp.turnos.catalog.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.unlp.turnos.catalog.catalog.CatalogApiIntegrationTest;
import ar.edu.unlp.turnos.catalog.shared.error.ErrorCodes;
import ar.edu.unlp.turnos.catalog.support.TestEnvironment;
import ar.edu.unlp.turnos.catalog.support.TestTokens;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * Authorization rules of the service (section 9 of the statement): who may reach each
 * endpoint and which answer each refusal produces.
 *
 * <p>The service is a stateless resource server: {@code 401} means "no usable identity"
 * (anonymous, broken signature, expired token or a token that is not ours) and
 * {@code 403 FORBIDDEN} means "a valid identity that does not hold {@code ROLE_USER}". Both
 * carry {@code application/problem+json} and a stable {@code code}, so a client never has to
 * parse the text.</p>
 */
class AuthorizationApiTest extends CatalogApiIntegrationTest {

    private static final String OTHER_SECRET = "another-secret-with-more-than-32-characters-long";

    @Test
    void anAnonymousRequestToAProtectedEndpointIsRejectedWith401ProblemJson() throws Exception {
        mockMvc.perform(get("/api/sync/status"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value(ErrorCodes.UNAUTHORIZED));

        mockMvc.perform(post("/api/internal/sync/run"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCodes.UNAUTHORIZED));

        mockMvc.perform(get("/api/account"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCodes.UNAUTHORIZED));
    }

    @Test
    void aTokenSignedWithAnotherSecretIsRejectedWith401() throws Exception {
        String token = TestTokens.hs256(OTHER_SECRET, "test.user");

        mockMvc.perform(get("/api/sync/status").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCodes.UNAUTHORIZED));
    }

    @Test
    void aTokenOfAnotherIssuerIsRejectedEvenWithOurSecret() throws Exception {
        String token = tokenWithoutIssuer();

        mockMvc.perform(get("/api/sync/status").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCodes.UNAUTHORIZED));
    }

    @Test
    void aTokenWithoutTheUserRoleIsAuthenticatedButForbidden() throws Exception {
        String token = TestTokens.hs256(TestEnvironment.TEST_JWT_SECRET, "test.user", List.of());

        mockMvc.perform(get("/api/sync/status").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(ErrorCodes.FORBIDDEN));

        mockMvc.perform(get("/api/account").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCodes.FORBIDDEN));
    }

    @Test
    void registrationAndAuthenticationAreReachableWithoutAToken() throws Exception {
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"anonimo.registro\",\"password\":\"segura-1234\","
                                + "\"firstName\":\"Anonimo\",\"lastName\":\"Registro\","
                                + "\"email\":\"anonimo.registro@example.com\",\"langKey\":\"es\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"anonimo.registro\",\"password\":\"segura-1234\","
                                + "\"rememberMe\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.id_token").isNotEmpty());
    }

    @Test
    void theHealthProbeIsReachableWithoutAToken() throws Exception {
        mockMvc.perform(get("/api/internal/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    /**
     * @return a token signed with the secret of this service but without our issuer: the
     *         decoder must refuse it, because a token of another system must not become a
     *         token of this one just because it shares the key
     */
    private static String tokenWithoutIssuer() {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject("test.user")
                    .claim("authorities", List.of("ROLE_USER"))
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + 3_600_000L))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(TestEnvironment.TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (Exception signingFailed) {
            throw new IllegalStateException("Could not sign the test token.", signingFailed);
        }
    }
}
