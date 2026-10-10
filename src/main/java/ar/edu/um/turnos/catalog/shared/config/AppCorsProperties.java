package ar.edu.um.turnos.catalog.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Browser origins allowed to call this API (comma separated).
 *
 * <p>Android clients do not enforce CORS, so an empty value (the default) is a valid and
 * secure configuration: it simply rejects every cross-origin browser request.</p>
 *
 * @param allowedOrigins e.g. {@code http://localhost:3000,http://localhost:5173}
 */
@ConfigurationProperties(prefix = "app.cors")
public record AppCorsProperties(String allowedOrigins) {
}
