package ar.edu.um.turnos.catalog.catalog.domain.model;

/**
 * Result of handling one {@code CatalogUpdated} notification.
 *
 * <p>It only describes what happened to the <em>message</em>: the effect on the local copy
 * (if any) is always the idempotent application of the pending versions.</p>
 */
public enum CatalogEventOutcome {

    /** The event had not been seen before: the pending versions were applied. */
    PROCESSED,

    /** The eventId was already recorded: no effect was repeated and the offset can be confirmed. */
    DUPLICATE
}
