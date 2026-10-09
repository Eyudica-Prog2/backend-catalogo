package ar.edu.unlp.turnos.catalog.shared.technical;

import ar.edu.unlp.turnos.catalog.shared.config.CatedraProperties;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * HTTP client used for every call to the catedra.
 *
 * <p>Timeouts come from the environment ({@code CATEDRA_HTTP_*}); a request without a
 * timeout would hang the service whenever the catedra network becomes black holed.</p>
 */
@Configuration
public class CatedraClientConfig {

    @Bean
    public RestClient catedraRestClient(CatedraProperties properties) {
        Duration connectTimeout = Duration.ofMillis(properties.http().connectTimeoutMs());
        Duration readTimeout = Duration.ofMillis(properties.http().readTimeoutMs());

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "backend-catalogo/0.1.0")
                .build();
    }
}
