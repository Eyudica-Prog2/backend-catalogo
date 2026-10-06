package ar.edu.unlp.turnos.catalog.shared.security;

import ar.edu.unlp.turnos.catalog.shared.config.AppJwtProperties;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Security rules of backend-catalogo.
 *
 * <p>Decisions:</p>
 * <ul>
 *   <li>stateless resource server: the end user JWT is issued by this service (HS256 with
 *       {@code APP_JWT_SECRET}) and validated here on every request;</li>
 *   <li>deny by default: only {@code GET /api/internal/health} (and the servlet error
 *       dispatch) are public;</li>
 *   <li>the technical JWT of the catedra is never accepted here, never stored and never
 *       exposed to KMP;</li>
 *   <li>401/403 are rendered as {@code application/problem+json} with a stable code.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Public endpoint. Everything else requires a valid end user JWT.
     */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/internal/health",
            "/error"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   CorsConfigurationSource corsConfigurationSource,
                                                   RestAuthenticationEntryPoint authenticationEntryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter)
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(authenticationEntryPoint))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }

    /**
     * HS256 decoder built from the shared secret. Fails at startup when the secret is shorter
     * than 256 bits, which is exactly what we want: no weak key silently accepted.
     */
    @Bean
    public JwtDecoder jwtDecoder(AppJwtProperties properties) {
        SecretKeySpec key = new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /**
     * Reads the authorities from the {@code authorities} claim, which is the shape produced by
     * JHipster and the one this project will emit in the user slice.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> authorities(jwt));
        return converter;
    }

    private static List<GrantedAuthority> authorities(Jwt jwt) {
        Object claim = jwt.getClaim("authorities");
        if (!(claim instanceof Iterable<?> values)) {
            return List.of();
        }
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (Object value : values) {
            if (value instanceof String authority && !authority.isBlank()) {
                authorities.add(new SimpleGrantedAuthority(authority));
            }
        }
        return authorities;
    }
}
