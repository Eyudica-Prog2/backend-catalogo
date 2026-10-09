package ar.edu.unlp.turnos.catalog.catalog.application.usecases;

import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogEventOutcome;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogUpdatedEvent;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.in.ProcessCatalogUpdatedUseCase;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.in.RunIncrementalSyncUseCase;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.out.ProcessedEventRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Applies a {@code CatalogUpdated} notification idempotently (contract v1, section 18.1).
 *
 * <p>The order of the two writes is the whole point of this class:</p>
 *
 * <ol>
 *   <li>the {@code eventId} is looked up first: a repeat is recognized before anything is
 *       touched, so a duplicated delivery confirms its offset without repeating effects;</li>
 *   <li>the pending versions are applied (each one in its own transaction, and applying the
 *       same version twice produces the same rows);</li>
 *   <li>only <strong>after</strong> that the {@code eventId} is recorded. A crash between 2
 *       and 3 redelivers the message, which finds nothing pending, records the id and
 *       confirms the offset. The opposite order could record an event whose effects were
 *       never persisted, hiding it forever.</li>
 * </ol>
 *
 * <p>The event never carries catalog data: it only announces that a new version exists.</p>
 */
@Component
@RequiredArgsConstructor
public class ProcessCatalogUpdatedUseCaseImpl implements ProcessCatalogUpdatedUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessCatalogUpdatedUseCaseImpl.class);

    private final ProcessedEventRepository processedEventRepository;
    private final RunIncrementalSyncUseCase runIncrementalSync;

    @Override
    public CatalogEventOutcome process(CatalogUpdatedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("A catalog notification cannot be empty.");
        }
        if (processedEventRepository.isProcessed(event.getEventId())) {
            log.info("Catalog notification already processed, skipping it: eventId={}, newVersion={}",
                    event.getEventId(), event.getNewVersion());
            return CatalogEventOutcome.DUPLICATE;
        }

        runIncrementalSync.runIncrementalSync();
        processedEventRepository.markProcessed(event.getEventId(), event.getNewVersion(), Instant.now());

        log.info("Catalog notification processed: eventId={}, newVersion={}",
                event.getEventId(), event.getNewVersion());
        return CatalogEventOutcome.PROCESSED;
    }
}
