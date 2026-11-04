package uk.co.techsavvysolutions.tickets.ticket.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PagedTest {

    @Test
    void totalPagesIsZeroWhenThereAreNoItems() {
        assertThat(new Paged<>(List.of(), 0, 10, 0).totalPages()).isZero();
    }

    @Test
    void totalPagesDoesNotAddAnEmptyPageForExactMultiples() {
        assertThat(new Paged<>(List.of(), 0, 10, 20).totalPages()).isEqualTo(2);
    }

    @Test
    void totalPagesRoundsUpForPartialPages() {
        assertThat(new Paged<>(List.of(), 0, 10, 21).totalPages()).isEqualTo(3);
    }
}
