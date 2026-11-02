package uk.co.techsavvysolutions.tickets.domain.ticket;

import java.time.Duration;

public final class DefaultSlaPolicy implements SlaPolicy {

    @Override
    public Duration responseTarget(TicketPriority priority) {
        return switch (priority) {
            case URGENT -> Duration.ofHours(1);
            case HIGH -> Duration.ofHours(4);
            case NORMAL -> Duration.ofHours(8);
            case LOW -> Duration.ofHours(24);
        };
    }
}
