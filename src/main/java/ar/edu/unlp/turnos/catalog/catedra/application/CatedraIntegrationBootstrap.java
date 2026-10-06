package ar.edu.unlp.turnos.catalog.catedra.application;

import ar.edu.unlp.turnos.catalog.catedra.application.exception.CatedraException;
import ar.edu.unlp.turnos.catalog.catedra.domain.model.CatedraIntegration;
import ar.edu.unlp.turnos.catalog.catedra.domain.ports.in.RefreshIntegrationUseCase;
import ar.edu.unlp.turnos.catalog.shared.config.CatedraProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Refreshes the technical integration during startup.
 *
 * <p>The service must boot even when the catedra is unreachable or not configured yet, so
 * every failure is logged as a warning (never as a fatal error) and never aborts the
 * startup. Tokens, passwords and redis secrets are not part of any log line.</p>
 */
@Component
@RequiredArgsConstructor
public class CatedraIntegrationBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CatedraIntegrationBootstrap.class);

    private final RefreshIntegrationUseCase refreshIntegration;
    private final CatedraProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.api().hasCredentials()) {
            log.info("Catedra technical credentials are not configured yet; skipping the startup integration refresh.");
            return;
        }
        try {
            CatedraIntegration integration = refreshIntegration.refresh();
            log.info("Startup integration refresh completed: groupId={}, provisioningStatus={}, redisHost={}, redisPort={}, kafkaBootstrapServers={}, catalogTopic={}",
                    integration.getGroupId(),
                    integration.getProvisioningStatus(),
                    integration.getRedisHost(),
                    integration.getRedisPort(),
                    integration.getKafkaBootstrapServers(),
                    integration.getKafkaCatalogTopic());
        } catch (CatedraException exception) {
            log.warn("Startup integration refresh failed: code={}, status={}, detail={}",
                    exception.getCode(), exception.getHttpStatus(), exception.getDetail());
        } catch (RuntimeException exception) {
            log.error("Unexpected error during the startup integration refresh: {}", exception.getMessage(),
                    exception);
        }
    }
}
