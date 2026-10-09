package ar.edu.unlp.turnos.catalog.shared.technical;

import ar.edu.unlp.turnos.catalog.shared.error.CatedraException;
import ar.edu.unlp.turnos.catalog.shared.config.CatedraProperties;
import java.util.function.Predicate;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Bounded retry with exponential backoff.
 *
 * <p>Only failures classified as retryable are repeated: an authentication rejection or a
 * contract violation must fail fast instead of burning attempts. The loop is always finite,
 * so no retry cycle can run forever.</p>
 */
@Component
@RequiredArgsConstructor
public class RetryExecutor {

    private final CatedraProperties properties;

    /**
     * Runs the given action, retrying only when the thrown runtime exception is retryable.
     *
     * @param operation human readable name of the call, used in logs and diagnostics
     * @param action    call to execute
     * @param retryable decides whether the failure is worth another attempt
     * @param <T>       result type
     * @return the first successful result
     */
    public <T> T execute(String operation, Supplier<T> action, Predicate<RuntimeException> retryable) {
        CatedraProperties.Retry policy = properties.retry();
        int maxAttempts = Math.max(1, policy.maxAttempts());

        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.get();
            } catch (RuntimeException failure) {
                if (!retryable.test(failure)) {
                    throw failure;
                }
                lastFailure = failure;
                if (attempt < maxAttempts) {
                    sleep(operation, policy.backoffFor(attempt));
                }
            }
        }
        throw lastFailure != null
                ? lastFailure
                : CatedraException.unavailable("The call to the catedra could not be completed.");
    }

    private void sleep(String operation, long backoffMillis) {
        if (backoffMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(backoffMillis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw CatedraException.unavailable("Interrupted while waiting to retry " + operation + ".");
        }
    }
}
