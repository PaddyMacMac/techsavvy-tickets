package uk.co.techsavvysolutions.tickets.domain.ticket;

import java.time.Duration;

/** Strategy: how quickly we promise a first reply for each priority. */
public interface SlaPolicy {

    Duration responseTarget(TicketPriority priority);
}
