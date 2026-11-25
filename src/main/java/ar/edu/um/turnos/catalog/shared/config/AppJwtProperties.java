package ar.edu.um.turnos.catalog.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * HS256 settings used to issue and validate the end user JWT.
 *
 * <p>The secret is shared with backend-turnos through {@code APP_JWT_SECRET}; it never has a
 * default value in {@code application.yml}, so the service refuses to boot without it.</p>
 *
 * <p>Two lifetimes: the regular one and the one used when the client asks to be remembered
 * at {@code POST /api/authenticate}. The reported {@code expires_in} always matches the
 * lifetime of the token that was issued.</p>
 *
 * @param secret                 HMAC secret, minimum 32 characters (256 bits)
 * @param expirySeconds          lifetime of a regular token
 * @param rememberMeExpirySeconds lifetime of a token issued with {@code rememberMe=true}
 */
@ConfigurationProperties(prefix = "app.jwt")
public record AppJwtProperties(String secret, long expirySeconds, long rememberMeExpirySeconds) {
}
