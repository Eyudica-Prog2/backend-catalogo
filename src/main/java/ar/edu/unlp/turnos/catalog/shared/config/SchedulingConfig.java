package ar.edu.unlp.turnos.catalog.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's task scheduler for the whole application.
 *
 * <p>It lives in {@code shared} because it is a process wide capability: any slice that
 * declares a {@code @Scheduled} method gets it. The only scheduled task of this service
 * today is the periodic catch-up of the catalog copy (the safety net for a lost Kafka
 * notification).</p>
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
