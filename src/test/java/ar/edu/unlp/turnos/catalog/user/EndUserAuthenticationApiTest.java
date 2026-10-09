package ar.edu.unlp.turnos.catalog.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.unlp.turnos.catalog.catalog.CatalogApiIntegrationTest;
import ar.edu.unlp.turnos.catalog.shared.error.ErrorCodes;
import ar.edu.unlp.turnos.catalog.user.domain.ports.out.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * Registration, authentication and account of the end user (sections 3.2, 8 and 9 of the
 * statement, section 13 of the reference).
 *
 * <p>Real context, real database and the real BCrypt/Nimbus adapters: what is proven here is
 * the contract the KMP consumes ({@code 201}, {@code id_token}, the claims of the token and
 * the codes of the three error cases) plus the two rules that are easy to get wrong, namely
 * that the password never leaves the server and that the same answer is given for an unknown
 * login and for a wrong password.</p>
 */
class EndUserAuthenticationApiTest extends CatalogApiIntegrationTest {

    private static final String LOGIN = "ana.perez";
    private static final String PASSWORD = "super-secret-1";
    private static final String EMAIL = "Ana.Perez@example.com";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerThenAuthenticateIssuesTheTokenOfTheContract() throws Exception {
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration(LOGIN, PASSWORD, "Ana", "Perez", EMAIL)))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));

        JsonNode token = authenticate(LOGIN, PASSWORD, false, 3600);

        SignedJWT jwt = SignedJWT.parse(token.get("id_token").asText());
        Date issuedAt = jwt.getJWTClaimsSet().getIssueTime();
        Date expiresAt = jwt.getJWTClaimsSet().getExpirationTime();

        // Claims required by this iteration, readable by every other service that shares the
        // secret.
        assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo(LOGIN);
        assertThat(jwt.getJWTClaimsSet().getIssuer()).isEqualTo("backend-catalogo");
        assertThat(jwt.getJWTClaimsSet().getStringClaim("publicId")).isNotBlank();
        assertThat(jwt.getJWTClaimsSet().getStringListClaim("authorities"))
                .containsExactly("ROLE_USER");
        assertThat((expiresAt.getTime() - issuedAt.getTime()) / 1000)
                .as("the reported expires_in must match the lifetime of the token")
                .isEqualTo(3600L);

        // The account can be read back with that token, without any credential in the body.
        String publicId = jwt.getJWTClaimsSet().getStringClaim("publicId");
        mockMvc.perform(get("/api/account")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.get("id_token").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value(LOGIN))
                .andExpect(jsonPath("$.firstName").value("Ana"))
                .andExpect(jsonPath("$.lastName").value("Perez"))
                .andExpect(jsonPath("$.email").value("ana.perez@example.com"))
                .andExpect(jsonPath("$.langKey").value("es"))
                .andExpect(jsonPath("$.activated").value(true))
                .andExpect(jsonPath("$.publicId").value(publicId))
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void registerTwiceWithTheSameLoginIsRejectedWithItsOwnCode() throws Exception {
        registerOk(LOGIN);

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration(LOGIN, "another-password", "Otra", "Persona",
                                "otra.persona@example.com")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(ErrorCodes.USERNAME_ALREADY_EXISTS));
    }

    @Test
    void registerTwiceWithTheSameEmailIsRejectedWithItsOwnCode() throws Exception {
        registerOk(LOGIN);

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration("otra.persona", "another-password", "Otra", "Persona",
                                // Same address in another case: uniqueness is not case sensitive.
                                EMAIL.toUpperCase())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.EMAIL_ALREADY_EXISTS));
    }

    @Test
    void registerWithInvalidDataIsRejectedAsAValidationError() throws Exception {
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration("ab", "123", "", "Perez", "not-an-email")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_ERROR))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'login')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'password')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'firstName')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").exists());
    }

    @Test
    void thePasswordIsNeverStoredInClearText() throws Exception {
        registerOk(LOGIN);

        assertThat(userRepository.findByLogin(LOGIN)).hasValueSatisfying(user -> {
            String stored = user.getPasswordHash();
            assertThat(stored).startsWith("$2").hasSizeGreaterThanOrEqualTo(60);
            assertThat(stored).isNotEqualTo(PASSWORD);
            assertThat(stored).doesNotContain(PASSWORD);
            assertThat(user.isActivated()).isTrue();
            assertThat(user.getAuthorities()).containsExactly("ROLE_USER");
            assertThat(user.getPublicId()).isNotNull();
        });
    }

    @Test
    void authenticatingWithTheWrongPasswordOrAnUnknownLoginAnswersTheSame401() throws Exception {
        registerOk(LOGIN);

        String wrongPassword = mockMvc.perform(post("/api/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(authentication(LOGIN, "definitely-not-it", false)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCodes.UNAUTHORIZED))
                .andReturn().getResponse().getContentAsString();

        String unknownLogin = mockMvc.perform(post("/api/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(authentication("nadie.tiene.este.login", PASSWORD, false)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCodes.UNAUTHORIZED))
                .andReturn().getResponse().getContentAsString();

        // Same status, same code and same detail: the endpoint cannot be used to discover
        // which logins exist.
        assertThat(wrongPassword).isEqualTo(unknownLogin);
        assertThat(wrongPassword).doesNotContain(PASSWORD);
        assertThat(wrongPassword).doesNotContain("definitely-not-it");
    }

    @Test
    void rememberMeIssuesTheConfiguredLongerLifetime() throws Exception {
        registerOk("recordarme");

        authenticate("recordarme", PASSWORD, true, 86400);
    }

    @Test
    void anEmptyBodyOfAuthenticationIsAValidationError() throws Exception {
        mockMvc.perform(post("/api/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_ERROR))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'username')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'password')]").exists());
    }

    private void registerOk(String login) throws Exception {
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration(login, PASSWORD, "Ana", "Perez",
                                login + "@example.com")))
                .andExpect(status().isCreated());
    }

    /**
     * @param login       account to authenticate
     * @param password    password presented
     * @param rememberMe  whether the longer lifetime is asked for
     * @param expectedLifetime lifetime in seconds the service must report
     * @return the body of the answer, ready to read {@code id_token}
     */
    private JsonNode authenticate(String login, String password, boolean rememberMe,
                                  long expectedLifetime) throws Exception {
        String body = mockMvc.perform(post("/api/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(authentication(login, password, rememberMe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(expectedLifetime))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private static String registration(String login, String password, String firstName,
                                       String lastName, String email) {
        return "{\"login\":\"" + login + "\""
                + ",\"password\":\"" + password + "\""
                + ",\"firstName\":\"" + firstName + "\""
                + ",\"lastName\":\"" + lastName + "\""
                + ",\"email\":\"" + email + "\""
                + ",\"langKey\":\"es\"}";
    }

    private static String authentication(String login, String password, boolean rememberMe) {
        return "{\"username\":\"" + login + "\",\"password\":\"" + password
                + "\",\"rememberMe\":" + rememberMe + "}";
    }
}
