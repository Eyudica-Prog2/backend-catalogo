package ar.edu.um.turnos.catalog.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Connection settings for the technical account held by this project at the catedra service.
 *
 * <p>Every value is externalized (see {@code application.yml}); nothing here is a default.
 * The nested {@link Redis} and {@link Kafka} records mirror the {@code integration} object of
 * contract v1 and are used as the expected values when the response of the catedra is
 * validated at startup.</p>
 */
@ConfigurationProperties(prefix = "catedra")
public record CatedraProperties(Api api, Http http, Retry retry, Redis redis, Kafka kafka) {

    /**
     * REST endpoint of the catedra plus the technical credentials.
     *
     * @param baseUrl  base URL, without trailing path
     * @param login    technical login ({@code CATEDRA_LOGIN}); optional when {@code jwt} is set
     * @param password technical password; optional when {@code jwt} is set
     * @param jwt      pre issued technical token; when set it replaces the login call
     */
    public record Api(String baseUrl, String login, String password, String jwt) {

        /**
         * @return true when this service is able to authenticate against the catedra
         */
        public boolean hasCredentials() {
            boolean hasLogin = notBlank(login) && notBlank(password);
            return notBlank(jwt) || hasLogin;
        }

        private static boolean notBlank(String value) {
            return value != null && !value.isBlank();
        }
    }

    /**
     * HTTP timeouts applied to every call to the catedra.
     *
     * @param connectTimeoutMs TCP connection timeout in milliseconds
     * @param readTimeoutMs    response timeout in milliseconds
     */
    public record Http(long connectTimeoutMs, long readTimeoutMs) {
    }

    /**
     * Bounded retry policy used when obtaining the technical token or the integration.
     *
     * @param maxAttempts      total attempts (first try included), minimum 1
     * @param initialBackoffMs delay before the second attempt
     * @param maxBackoffMs     upper bound of the exponential backoff
     */
    public record Retry(int maxAttempts, long initialBackoffMs, long maxBackoffMs) {

        /**
         * @param attempt 1 based number of the attempt that just failed
         * @return milliseconds to wait before the next attempt
         */
        public long backoffFor(int attempt) {
            if (attempt < 1) {
                return 0;
            }
            long factor = 1L << Math.min(attempt - 1, 20);
            return Math.min(maxBackoffMs, initialBackoffMs * factor);
        }
    }

    /**
     * Redis coordinates reported by the catedra.
     *
     * @param host           redis host
     * @param port           redis port
     * @param username       redis ACL user ({@code grp_...})
     * @param password       redis password; only delivered when provisioning is PROVISIONED
     * @param readNamespace  readable namespace, e.g. {@code catedra:sync:*}
     * @param writeNamespace private namespace of this project
     */
    public record Redis(String host, int port, String username, String password,
                        String readNamespace, String writeNamespace) {
    }

    /**
     * Kafka coordinates reported by the catedra.
     *
     * @param bootstrapServers broker list
     * @param consumerGroupId  suggested consumer group
     * @param catalogTopic     topic carrying {@code CatalogUpdated} events
     * @param actionsTopic     topic carrying reservation requests and results
     * @param phoneTopic       topic used to publish the additional phone information
     */
    public record Kafka(String bootstrapServers, String consumerGroupId, String catalogTopic,
                        String actionsTopic, String phoneTopic) {
    }
}
