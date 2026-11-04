package uk.co.techsavvysolutions.tickets.ticket.domain;

import java.time.Duration;

/** Strategy: how quickly we promise a first reply for each priority. */
public interface SlaPolicy {

    Duration responseTarget(TicketPriority priority);
}
