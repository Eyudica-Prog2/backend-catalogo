package ar.edu.unlp.turnos.catalog.catalog.application;

import ar.edu.unlp.turnos.catalog.catalog.domain.ports.in.RunIncrementalSyncUseCase;
import ar.edu.unlp.turnos.catalog.shared.error.ApiException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodic reconciliation of the local copy against Redis (statement sections 6 and 8,
 * reference section 16).
 *
 * <p>Kafka only <em>notifies</em>: a lost, skipped or undelivered {@code CatalogUpdated}
 * message would otherwise leave this service behind forever. Comparing the local version
 * with {@code current-version} on a fixed delay recovers from that loss without any human
 * intervention, and it is bounded: when there is nothing pending the run reads one window
 * and does nothing.</p>
 *
 * <p>Failures are logged and recorded by the use case (visible through
 * {@code GET /api/sync/status}); they never propagate, because a scheduler that throws
 * would only add stack traces without changing the outcome. Disable it with
 * {@code app.sync.scheduler.enabled=false}.</p>
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.sync.scheduler", name = "enabled", havingValue = "true",
        matchIfMissing = true)
public class CatalogSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(CatalogSyncScheduler.class);

    private final RunIncrementalSyncUseCase runIncrementalSync;

    /**
     * Applies whatever Redis published since the last successful run.
     */
    @Scheduled(initialDelayString = "${app.sync.scheduler.initial-delay-ms:60000}",
            fixedDelayString = "${app.sync.scheduler.fixed-delay-ms:300000}")
    public void catchUpWithRedis() {
        try {
            runIncrementalSync.runIncrementalSync();
        } catch (ApiException expected) {
            log.warn("Periodic catalog synchronization did not complete: code={}, status={}, detail={}",
                    expected.getCode(), expected.getHttpStatus(), expected.getDetail());
        } catch (RuntimeException unexpected) {
            log.warn("Periodic catalog synchronization failed: {}", unexpected.getMessage());
        }
    }
}
