package ar.edu.unlp.turnos.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point of backend-catalogo.
 *
 * <p>Package layout follows vertical slicing: each business capability owns its
 * {@code domain}, {@code application} and {@code infrastructure} folders. Cross cutting
 * concerns live in {@code shared}. The capabilities implemented so far:</p>
 *
 * <ul>
 *   <li>{@code catedra}: technical integration with the catedra (status and refresh of the
 *       provisioning of this project);</li>
 *   <li>{@code catalog}: local copy of the catalog (snapshot download and application, status
 *       and forced synchronization, public search and the internal contract consumed by
 *       {@code backend-turnos}). Synchronization lives here on purpose: it writes the tables
 *       this slice owns, and a separate {@code sync} slice would have to reach across the
 *       boundary to do it.</li>
 * </ul>
 *
 * <p>A {@code user} slice is expected later; it will keep the same shape.</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class CatalogApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogApplication.class, args);
    }
}
