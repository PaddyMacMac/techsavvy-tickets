package uk.co.techsavvysolutions.tickets.domain.ticket;

/** Which tickets a person may see. Engineers see everything; clients see only what they raised. */
public sealed interface TicketScope {

    record AllTickets() implements TicketScope {}

    record OwnTickets(String requesterSubject) implements TicketScope {}
}
