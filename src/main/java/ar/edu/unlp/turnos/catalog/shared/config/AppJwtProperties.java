package ar.edu.unlp.turnos.catalog.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * HS256 settings used to issue and validate the end user JWT.
 *
 * <p>The secret is shared with backend-turnos through {@code APP_JWT_SECRET}; it never has a
 * default value in {@code application.yml}, so the service refuses to boot without it.</p>
 *
 * @param secret        HMAC secret, minimum 32 characters (256 bits)
 * @param expirySeconds lifetime of the tokens issued to end users
 */
@ConfigurationProperties(prefix = "app.jwt")
public record AppJwtProperties(String secret, long expirySeconds) {
}
