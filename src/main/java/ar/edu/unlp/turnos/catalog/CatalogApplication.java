package ar.edu.unlp.turnos.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point of backend-catalogo.
 *
 * <p>Package layout follows vertical slicing: each business capability ({@code catedra},
 * {@code catalog}, {@code sync}, {@code user}) owns its {@code domain}, {@code application}
 * and {@code infrastructure} folders. Cross cutting concerns live in {@code shared}.</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class CatalogApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogApplication.class, args);
    }
}
