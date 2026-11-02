package uk.co.techsavvysolutions.tickets.api.ticket;

import uk.co.techsavvysolutions.tickets.application.ticket.LogTicketCommand;
import org.springframework.stereotype.Component;

/** Converts and normalizes the HTTP request at the API boundary. */
@Component
class LogTicketRequestMapper {

    LogTicketCommand toCommand(LogTicketRequest request) {
        return new LogTicketCommand(
                request.title().strip(),
                request.description().strip(),
                request.category(),
                request.priority(),
                request.companyName().strip(),
                request.requiredBy());
    }
}
