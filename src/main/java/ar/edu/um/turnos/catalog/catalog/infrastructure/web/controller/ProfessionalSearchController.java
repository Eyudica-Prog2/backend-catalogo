package ar.edu.um.turnos.catalog.catalog.infrastructure.web.controller;

import ar.edu.um.turnos.catalog.catalog.domain.model.ProfessionalPage;
import ar.edu.um.turnos.catalog.catalog.domain.model.ProfessionalSearchCriteria;
import ar.edu.um.turnos.catalog.catalog.domain.ports.in.GetProfessionalByIdUseCase;
import ar.edu.um.turnos.catalog.catalog.domain.ports.in.GetProfessionalCategoriesUseCase;
import ar.edu.um.turnos.catalog.catalog.domain.ports.in.SearchProfessionalsUseCase;
import ar.edu.um.turnos.catalog.catalog.infrastructure.web.dto.ProfessionalCategoryResponse;
import ar.edu.um.turnos.catalog.catalog.infrastructure.web.dto.ProfessionalDetailResponse;
import ar.edu.um.turnos.catalog.catalog.infrastructure.web.dto.ProfessionalResponse;
import ar.edu.um.turnos.catalog.catalog.infrastructure.web.mapper.ProfessionalDtoMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Public API over the local copy of the catalog (section 6 of the statement).
 *
 * <p>The controller only parses the request, delegates to a use case port and assembles the
 * response: validation rules, business decisions and error conversions stay behind it and are
 * reached through the shared {@code GlobalExceptionHandler}, never with a try/catch here.</p>
 *
 * <p>The paginated search answers two headers required by the statement:</p>
 * <ul>
 *   <li>{@code X-Total-Count}: total number of professionals matching the filters, which is
 *       what a client uses to render the page controls;</li>
 *   <li>{@code Link}: {@code first}, {@code prev}, {@code next} and {@code last}, built from
 *       the request that was actually received so every filter is kept when paging.</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
public class ProfessionalSearchController {

    /** Header with the total number of matches, not with the size of the page. */
    static final String X_TOTAL_COUNT = "X-Total-Count";

    private final GetProfessionalCategoriesUseCase getProfessionalCategories;
    private final SearchProfessionalsUseCase searchProfessionals;
    private final GetProfessionalByIdUseCase getProfessionalById;
    private final ProfessionalDtoMapper mapper;

    /**
     * Every professional category of the local copy, including the disabled ones.
     *
     * @return 200 with the categories
     */
    @GetMapping("/api/professional-categories")
    public ResponseEntity<List<ProfessionalCategoryResponse>> getProfessionalCategories() {
        return ResponseEntity.ok(
                mapper.toCategoryResponses(getProfessionalCategories.getProfessionalCategories()));
    }

    /**
     * Searches professionals over the local copy with the four mandatory filters.
     *
     * @param categoryId mandatory filter: category of the professional
     * @param name       mandatory filter: partial match on first and last name
     * @param enabled    mandatory filter: enabled state of the professional
     * @param available  mandatory filter: professional with at least one enabled schedule
     * @param page       zero based page index
     * @param size       page size, default 20, maximum 100
     * @param sort       {@code property} or {@code property,asc|desc}
     * @return 200 with the page, X-Total-Count and Link
     */
    @GetMapping("/api/professionals")
    public ResponseEntity<List<ProfessionalResponse>> searchProfessionals(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Boolean available,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastName,asc") String sort) {

        ProfessionalSearchCriteria criteria =
                mapper.toCriteria(categoryId, name, enabled, available, page, size, sort);
        ProfessionalPage result = searchProfessionals.searchProfessionals(criteria);

        return ResponseEntity.ok()
                .header(X_TOTAL_COUNT, String.valueOf(result.totalElements()))
                .header(HttpHeaders.LINK, linkHeader(result))
                .body(mapper.toResponses(result.content()));
    }

    /**
     * Reads one professional together with its weekly schedules.
     *
     * @param id remote id of the professional
     * @return 200 with the professional and its schedules, 404 when it is not part of the
     *         local copy
     */
    @GetMapping("/api/professionals/{id}")
    public ResponseEntity<ProfessionalDetailResponse> getProfessionalById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(mapper.toDetailResponse(getProfessionalById.getProfessionalById(id)));
    }

    /**
     * Builds the {@code Link} header of a page: first and last are always present, prev and
     * next only when they exist. The current request is reused so every filter the client
     * sent is carried over to the neighbouring pages.
     *
     * @param page page that was returned
     * @return the value of the header
     */
    private String linkHeader(ProfessionalPage page) {
        List<String> links = new ArrayList<>();
        links.add(link("first", 0, page.size()));
        if (page.hasPrevious()) {
            links.add(link("prev", page.page() - 1, page.size()));
        }
        if (page.hasNext()) {
            links.add(link("next", page.page() + 1, page.size()));
        }
        links.add(link("last", Math.max(0, page.totalPages() - 1), page.size()));
        return String.join(", ", links);
    }

    private String link(String rel, long pageNumber, int size) {
        String uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .replaceQueryParam("page", pageNumber)
                .replaceQueryParam("size", size)
                .build()
                .toUriString();
        return "<" + uri + ">; rel=\"" + rel + "\"";
    }
}
