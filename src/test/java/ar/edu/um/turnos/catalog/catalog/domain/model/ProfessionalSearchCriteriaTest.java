package ar.edu.um.turnos.catalog.catalog.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Unit test of the criteria of the professional search: normalization of the values a client
 * may send and rejection of the ones that cannot be used.
 */
class ProfessionalSearchCriteriaTest {

    @Test
    void normalizesTheNameAndKeepsTheFiltersUntouched() {
        ProfessionalSearchCriteria criteria = new ProfessionalSearchCriteria(
                10L, "  ana  ", Boolean.TRUE, Boolean.FALSE, 0, 20, "lastName", SortDirection.DESC);

        assertThat(criteria.name()).isEqualTo("ana");
        assertThat(criteria.namePattern()).isEqualTo("%ana%");
        assertThat(criteria.categoryId()).isEqualTo(10L);
        assertThat(criteria.enabled()).isTrue();
        assertThat(criteria.available()).isFalse();
        assertThat(criteria.sortProperty()).isEqualTo("lastName");
        assertThat(criteria.sortDirection()).isEqualTo(SortDirection.DESC);
    }

    @Test
    void treatsABlankNameAsNoNameFilter() {
        assertThat(new ProfessionalSearchCriteria(null, "   ", null, null, 0, 20, null, null).name())
                .isNull();
        assertThat(new ProfessionalSearchCriteria(null, null, null, null, 0, 20, null, null).namePattern())
                .isNull();
    }

    @Test
    void defaultsToTheLastNameOrderWhenTheClientDoesNotAskForOne() {
        ProfessionalSearchCriteria criteria = new ProfessionalSearchCriteria(
                null, null, null, null, 0, ProfessionalSearchCriteria.DEFAULT_SIZE, null, null);

        assertThat(criteria.sortProperty()).isEqualTo("lastName");
        assertThat(criteria.sortDirection()).isEqualTo(SortDirection.ASC);
    }

    @Test
    void rejectsAPageThatCannotExist() {
        assertThatThrownBy(() -> new ProfessionalSearchCriteria(
                null, null, null, null, -1, 20, "lastName", SortDirection.ASC))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("page must not be negative");
    }

    @Test
    void rejectsAPageOutsideTheAllowedRange() {
        assertThatThrownBy(() -> new ProfessionalSearchCriteria(
                null, null, null, null, 0, 0, "lastName", SortDirection.ASC))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size must be between 1 and 100");

        assertThatThrownBy(() -> new ProfessionalSearchCriteria(
                null, null, null, null, 0, 101, "lastName", SortDirection.ASC))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size must be between 1 and 100");
    }

    @Test
    void rejectsSortPropertiesThatAreNotPartOfTheWhitelist() {
        assertThatThrownBy(() -> new ProfessionalSearchCriteria(
                null, null, null, null, 0, 20, "password", SortDirection.ASC))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sort must be one of");
    }

    @Test
    void onlyAcceptsTheDirectionsTheContractDefines() {
        assertThatCode(() -> SortDirection.from("desc")).doesNotThrowAnyException();
        assertThatCode(() -> SortDirection.from("ASC")).doesNotThrowAnyException();
        assertThatCode(() -> SortDirection.from(" ")).doesNotThrowAnyException();

        assertThatThrownBy(() -> SortDirection.from("sideways"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exposesThePaginationLimitsAsPartOfTheDomain() {
        assertThat(ProfessionalSearchCriteria.MAX_SIZE).isEqualTo(100);
        assertThat(ProfessionalSearchCriteria.DEFAULT_SIZE).isEqualTo(20);
        assertThat(ProfessionalSearchCriteria.SORTABLE_PROPERTIES)
                .containsExactlyInAnyOrder("id", "firstName", "lastName");
    }
}
