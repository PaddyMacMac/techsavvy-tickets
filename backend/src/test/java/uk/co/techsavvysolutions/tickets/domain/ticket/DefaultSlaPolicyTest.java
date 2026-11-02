package uk.co.techsavvysolutions.tickets.domain.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DefaultSlaPolicyTest {

    private final SlaPolicy policy = new DefaultSlaPolicy();

    @ParameterizedTest
    @CsvSource({"URGENT,1", "HIGH,4", "NORMAL,8", "LOW,24"})
    void promisesFasterRepliesForMoreUrgentWork(TicketPriority priority, long hours) {
        assertThat(policy.responseTarget(priority)).isEqualTo(Duration.ofHours(hours));
    }
}
