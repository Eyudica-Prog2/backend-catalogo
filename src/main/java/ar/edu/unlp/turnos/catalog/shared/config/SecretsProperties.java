package ar.edu.unlp.turnos.catalog.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Source material for symmetric encryption of persisted secrets.
 *
 * <p>The value of {@code APP_SECRETS_KEY} is never used directly: SHA-256 derives the
 * AES-256 key, so any long random phrase works without caring about key sizes.</p>
 *
 * @param key base secret, never committed to the repository
 */
@ConfigurationProperties(prefix = "app.secrets")
public record SecretsProperties(String key) {
}
