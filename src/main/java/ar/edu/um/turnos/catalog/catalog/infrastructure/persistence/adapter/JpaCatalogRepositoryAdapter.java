package ar.edu.um.turnos.catalog.catalog.infrastructure.persistence.adapter;

import ar.edu.um.turnos.catalog.catalog.domain.model.Professional;
import ar.edu.um.turnos.catalog.catalog.domain.model.ProfessionalCategory;
import ar.edu.um.turnos.catalog.catalog.domain.model.ProfessionalDetails;
import ar.edu.um.turnos.catalog.catalog.domain.model.ProfessionalPage;
import ar.edu.um.turnos.catalog.catalog.domain.model.ProfessionalSearchCriteria;
import ar.edu.um.turnos.catalog.catalog.domain.model.SortDirection;
import ar.edu.um.turnos.catalog.catalog.domain.ports.out.CatalogRepository;
import ar.edu.um.turnos.catalog.catalog.infrastructure.persistence.entity.ProfessionalEntity;
import ar.edu.um.turnos.catalog.catalog.infrastructure.persistence.entity.WeeklyScheduleEntity;
import ar.edu.um.turnos.catalog.catalog.infrastructure.persistence.mapper.CatalogMapper;
import ar.edu.um.turnos.catalog.catalog.infrastructure.persistence.repository.JpaProfessionalCategoryRepository;
import ar.edu.um.turnos.catalog.catalog.infrastructure.persistence.repository.JpaProfessionalRepository;
import ar.edu.um.turnos.catalog.catalog.infrastructure.persistence.repository.JpaWeeklyScheduleRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA adapter for the {@link CatalogRepository} port: every read of the local copy.
 *
 * <p>The four mandatory filters are combined in a single query. The availability filter is a
 * semi-join over {@code weekly_schedule}: {@code available=true} selects the professionals
 * with at least one enabled schedule, {@code available=false} the ones without any.</p>
 */
@Component
@RequiredArgsConstructor
public class JpaCatalogRepositoryAdapter implements CatalogRepository {

    private final JpaProfessionalCategoryRepository categoryRepository;
    private final JpaProfessionalRepository professionalRepository;
    private final JpaWeeklyScheduleRepository weeklyScheduleRepository;
    private final CatalogMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<ProfessionalCategory> findAllCategories() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProfessionalPage searchProfessionals(ProfessionalSearchCriteria criteria) {
        Specification<ProfessionalEntity> specification = buildSpecification(criteria);
        Pageable pageable = PageRequest.of(criteria.page(), criteria.size(), orderOf(criteria));

        List<Professional> content = professionalRepository.findAll(specification, pageable)
                .stream()
                .map(mapper::toDomain)
                .toList();
        long totalElements = professionalRepository.count(specification);

        return new ProfessionalPage(content, totalElements, criteria.page(), criteria.size());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProfessionalDetails> findProfessionalById(Long professionalId) {
        return professionalRepository.findById(professionalId)
                .map(entity -> new ProfessionalDetails(
                        mapper.toDomain(entity),
                        weeklyScheduleRepository
                                .findAllByProfessionalIdOrderByDayOfWeekAscStartTimeAsc(professionalId)
                                .stream()
                                .map(mapper::toDomain)
                                .toList()));
    }

    /**
     * Translates the criteria into one specification. Unknown sort properties cannot reach
     * this point: the domain criteria only accepts the properties of
     * {@link ProfessionalSearchCriteria#SORTABLE_PROPERTIES} and {@link #orderOf} restricts
     * them again, so no request can make Spring Data throw a property reference error.
     */
    private Specification<ProfessionalEntity> buildSpecification(ProfessionalSearchCriteria criteria) {
        Specification<ProfessionalEntity> specification = Specification.where(null);

        if (criteria.categoryId() != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("categoryId"), criteria.categoryId()));
        }
        if (criteria.name() != null) {
            String pattern = criteria.namePattern();
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("firstName")), pattern),
                    cb.like(cb.lower(root.get("lastName")), pattern)));
        }
        if (criteria.enabled() != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("enabled"), criteria.enabled()));
        }
        if (criteria.available() != null) {
            specification = specification.and(hasEnabledSchedule(criteria.available()));
        }
        return specification;
    }

    /**
     * @param expected true to keep the professionals with an enabled schedule, false for the
     *                 ones without any
     * @return specification backed by an EXISTS sub-query over {@code weekly_schedule}
     */
    private static Specification<ProfessionalEntity> hasEnabledSchedule(boolean expected) {
        return (root, query, cb) -> {
            var schedule = query.subquery(WeeklyScheduleEntity.class);
            var scheduleRoot = schedule.from(WeeklyScheduleEntity.class);
            schedule.select(scheduleRoot.get("id"));
            schedule.where(
                    cb.equal(scheduleRoot.get("professionalId"), root.get("id")),
                    cb.isTrue(scheduleRoot.get("enabled")));
            return expected ? cb.exists(schedule) : cb.not(cb.exists(schedule));
        };
    }

    /**
     * @param criteria requested order
     * @return a whitelist backed order, always with {@code id} as tie breaker so pages never
     *         repeat or skip a row
     */
    private static Sort orderOf(ProfessionalSearchCriteria criteria) {
        Sort.Direction direction = criteria.sortDirection() == SortDirection.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        String property = ProfessionalSearchCriteria.SORTABLE_PROPERTIES.contains(criteria.sortProperty())
                ? criteria.sortProperty()
                : "lastName";
        return Sort.by(direction, property).and(Sort.by(direction, "id"));
    }
}
