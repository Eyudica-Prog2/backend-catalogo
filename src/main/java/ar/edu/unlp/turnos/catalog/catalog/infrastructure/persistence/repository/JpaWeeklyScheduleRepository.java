package ar.edu.unlp.turnos.catalog.catalog.infrastructure.persistence.repository;

import ar.edu.unlp.turnos.catalog.catalog.infrastructure.persistence.entity.WeeklyScheduleEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository. The domain never imports this type: it is reached only through the
 * adapter of this package.
 */
public interface JpaWeeklyScheduleRepository extends JpaRepository<WeeklyScheduleEntity, Long> {

    /**
     * Schedules of one professional, in a deterministic order (weekday, then start time).
     *
     * @param professionalId remote id of the professional
     * @return the schedules of that professional
     */
    List<WeeklyScheduleEntity> findAllByProfessionalIdOrderByDayOfWeekAscStartTimeAsc(Long professionalId);
}
