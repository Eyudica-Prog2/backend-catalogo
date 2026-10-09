package ar.edu.unlp.turnos.catalog.catalog.domain.ports.in;

import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogEventOutcome;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogUpdatedEvent;

/**
 * Applies one {@code CatalogUpdated} notification.
 *
 * <p>The event is only a hint that a new version exists: the effects of this case are the
 * idempotent application of the pending versions plus the recording of the {@code eventId}
 * in the local idempotency ledger.</p>
 */
public interface ProcessCatalogUpdatedUseCase {

    /**
     * @param event validated notification
     * @return {@link CatalogEventOutcome#DUPLICATE} when the eventId was already applied (no
     *         effect was repeated), {@link CatalogEventOutcome#PROCESSED} otherwise
     * @throws RuntimeException when the pending versions could not be applied; in that case
     *                          the eventId is <strong>not</strong> recorded, so the message
     *                          can be redelivered and retried
     */
    CatalogEventOutcome process(CatalogUpdatedEvent event);
}
