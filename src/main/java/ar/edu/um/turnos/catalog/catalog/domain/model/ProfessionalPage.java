package ar.edu.um.turnos.catalog.catalog.domain.model;

import java.util.List;

/**
 * Result of a paginated search over the local copy of the catalog.
 *
 * <p>Domain level pagination: the domain never imports Spring Data types such as
 * {@code Page} or {@code Pageable}.</p>
 *
 * @param content       professionals of the requested page
 * @param totalElements total number of professionals matching the criteria
 * @param page          zero based index of the returned page
 * @param size          maximum number of elements per page
 */
public record ProfessionalPage(List<Professional> content, long totalElements, int page, int size) {

    public ProfessionalPage {
        content = content == null ? List.of() : List.copyOf(content);
    }

    /**
     * @return true when a page after this one exists
     */
    public boolean hasNext() {
        return (long) (page + 1) * size < totalElements;
    }

    /**
     * @return true when a page before this one exists
     */
    public boolean hasPrevious() {
        return page > 0;
    }

    /**
     * @return number of pages, at least one even when there is no data
     */
    public long totalPages() {
        return size <= 0 ? 0 : Math.max(1, (totalElements + size - 1) / size);
    }
}
