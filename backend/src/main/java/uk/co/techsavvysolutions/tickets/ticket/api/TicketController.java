package uk.co.techsavvysolutions.tickets.ticket.api;

import uk.co.techsavvysolutions.tickets.ticket.application.ChangeTicketStatusService;
import uk.co.techsavvysolutions.tickets.ticket.application.LogTicketService;
import uk.co.techsavvysolutions.tickets.ticket.application.TicketQueryService;
import uk.co.techsavvysolutions.tickets.ticket.application.TicketSummaryService;
import uk.co.techsavvysolutions.tickets.ticket.domain.PageQuery;
import uk.co.techsavvysolutions.tickets.ticket.domain.TicketStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Slf4j
class TicketController {

    private final LogTicketService logTicketService;
    private final ChangeTicketStatusService changeTicketStatusService;
    private final TicketQueryService ticketQueryService;
    private final TicketSummaryService ticketSummaryService;
    private final LogTicketRequestMapper logTicketRequestMapper;
    private final TicketResponseMapper ticketResponseMapper;

    @PostMapping
    ResponseEntity<TicketResponse> logTicket(@Valid @RequestBody LogTicketRequest request) {
        var ticket = logTicketService.logTicket(logTicketRequestMapper.toCommand(request));
        log.info("Ticket {} logged for {} ({} priority)",
                ticket.getReference(), ticket.getCompanyName(), ticket.getPriority());
        return ResponseEntity.created(URI.create("/api/tickets/" + ticket.getId())).body(ticketResponseMapper.toResponse(ticket));
    }

    @GetMapping
    TicketPageResponse listTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int pageNumber,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        var pageQuery = new PageQuery(pageNumber, pageSize);
        return ticketResponseMapper.toPageResponse(ticketQueryService.listTickets(status, pageQuery));
    }

    @GetMapping("/summary")
    TicketSummaryResponse getSummary() {
        return ticketResponseMapper.toSummaryResponse(ticketSummaryService.getSummary());
    }

    @GetMapping("/{ticketId}")
    TicketResponse getTicket(@PathVariable UUID ticketId) {
        return ticketResponseMapper.toResponse(ticketQueryService.getTicketById(ticketId));
    }

    @PatchMapping("/{ticketId}/status")
    TicketResponse changeTicketStatus(@PathVariable UUID ticketId, @Valid @RequestBody ChangeTicketStatusRequest request) {
        var ticket = changeTicketStatusService.changeTicketStatus(ticketId, request.status());
        log.info("Ticket {} status changed to {}", ticket.getReference(), ticket.getStatus());
        return ticketResponseMapper.toResponse(ticket);
    }
}
