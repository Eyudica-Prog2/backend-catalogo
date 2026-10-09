package ar.edu.unlp.turnos.catalog.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * Guards the "no hardcoded configuration" rule of this iteration.
 *
 * <p>It parses {@code application.yml} and checks that every sensitive value is a
 * {@code ${ENV_VAR}} placeholder, that the placeholders have no embedded default, and that
 * {@code .env.example} documents every variable the service needs.</p>
 */
class ExternalizedConfigurationTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("^\\$\\{([A-Z0-9_]+)(?::[^{}]*)?}$");
    private static final Pattern ANY_PLACEHOLDER = Pattern.compile("\\$\\{([A-Z0-9_]+)(?::[^{}]*)?}");

    @Test
    void sensitiveSettingsArePlainEnvironmentPlaceholders() throws IOException {
        Map<String, Object> yaml = loadYaml();

        List<String> mustBeExternalized = List.of(
                "spring.datasource.url",
                "spring.datasource.username",
                "spring.datasource.password",
                "app.jwt.secret",
                "app.jwt.expiry-seconds",
                "app.secrets.key",
                "catedra.api.base-url",
                "catedra.http.connect-timeout-ms",
                "catedra.http.read-timeout-ms",
                "catedra.redis.host",
                "catedra.redis.port",
                "catedra.redis.username",
                "catedra.redis.password",
                "catedra.redis.read-namespace",
                "catedra.redis.write-namespace",
                "catedra.kafka.bootstrap-servers",
                "catedra.kafka.consumer-group-id",
                "catedra.kafka.catalog-topic",
                "catedra.kafka.actions-topic",
                "catedra.kafka.phone-topic");

        for (String path : mustBeExternalized) {
            Object value = navigate(yaml, path);
            assertThat(value).as("application.yml -> %s", path).isInstanceOf(String.class);
            assertThat((String) value)
                    .as("application.yml -> %s must be an environment placeholder without a default", path)
                    .matches(PLACEHOLDER.pattern());
        }
    }

    @Test
    void everyDocumentedVariableIsPresentInTheExampleEnvironmentFile() throws IOException {
        Path example = Path.of(".env.example");
        assertThat(Files.exists(example)).as(".env.example must exist next to pom.xml").isTrue();

        String content = Files.readString(example, StandardCharsets.UTF_8);
        Set<String> documented = new HashSet<>();
        Matcher matcher = Pattern.compile("^([A-Z0-9_]+)=", Pattern.MULTILINE).matcher(content);
        while (matcher.find()) {
            documented.add(matcher.group(1));
        }

        String yamlText = readApplicationYamlText();
        Set<String> required = new HashSet<>();
        Matcher placeholders = ANY_PLACEHOLDER.matcher(yamlText);
        while (placeholders.find()) {
            required.add(placeholders.group(1));
        }

        assertThat(required).as("variables referenced by application.yml").isNotEmpty();
        assertThat(required)
                .as("every variable used in application.yml must be documented in .env.example")
                .isSubsetOf(documented);
    }

    @Test
    void noSecretValueIsWrittenInPlainTextInTheConfiguration() throws IOException {
        Map<String, Object> yaml = loadYaml();

        assertThat((String) navigate(yaml, "app.jwt.secret")).doesNotContain("=");
        assertThat((String) navigate(yaml, "app.secrets.key")).doesNotContain("=");
        assertThat(yaml.toString()).doesNotContain("BEGIN RSA");
        assertThat(readApplicationYamlText()).doesNotContain("eyJhbGciOi"); // no embedded JWT
    }

    private static String readApplicationYamlText() throws IOException {
        try (InputStream stream = ExternalizedConfigurationTest.class.getClassLoader()
                .getResourceAsStream("application.yml")) {
            assertThat(stream).as("application.yml must be on the classpath").isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadYaml() throws IOException {
        try (InputStream stream = ExternalizedConfigurationTest.class.getClassLoader()
                .getResourceAsStream("application.yml")) {
            assertThat(stream).as("application.yml must be on the classpath").isNotNull();
            return new Yaml().load(stream);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object navigate(Map<String, Object> root, String path) {
        Map<String, Object> current = new LinkedHashMap<>(root);
        String[] segments = path.split("\\.");
        for (int i = 0; i < segments.length - 1; i++) {
            Object next = current.get(segments[i]);
            assertThat(next).as("missing node %s", segments[i]).isInstanceOf(Map.class);
            current = (Map<String, Object>) next;
        }
        return current.get(segments[segments.length - 1]);
    }
}
