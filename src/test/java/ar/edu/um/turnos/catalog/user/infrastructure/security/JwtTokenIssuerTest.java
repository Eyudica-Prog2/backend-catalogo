package ar.edu.um.turnos.catalog.user.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jwt.SignedJWT;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import ar.edu.um.turnos.catalog.shared.config.AppJwtProperties;
import ar.edu.um.turnos.catalog.user.domain.model.EndUser;

class JwtTokenIssuerTest {

    @Test
    void includesProfileNamesNeededByTurnosWithoutAddingOtherProfileData() throws Exception {
        String secret = "test-only-app-jwt-secret-with-minimum-length";
        JwtTokenIssuer issuer = new JwtTokenIssuer(new AppJwtProperties(secret, 3600, 86_400));
        EndUser user = EndUser.builder()
                .login("ana")
                .publicId(UUID.fromString("a43d7a0b-fec6-4f3a-98c6-d2e283194bd9"))
                .firstName("Ana")
                .lastName("Perez")
                .email("ana@example.test")
                .authorities(List.of(EndUser.DEFAULT_AUTHORITY))
                .build();

        SignedJWT token = SignedJWT.parse(issuer.issue(user, false).getValue());

        assertThat(token.getJWTClaimsSet().getStringClaim("firstName")).isEqualTo("Ana");
        assertThat(token.getJWTClaimsSet().getStringClaim("lastName")).isEqualTo("Perez");
        assertThat(token.getJWTClaimsSet().getStringClaim("email")).isNull();
        assertThat(token.getJWTClaimsSet().getStringClaim("password")).isNull();
    }
}
