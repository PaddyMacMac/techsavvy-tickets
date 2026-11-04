package uk.co.techsavvysolutions.tickets.ticket.domain;

import uk.co.techsavvysolutions.tickets.ticket.domain.errors.InvalidStatusTransitionException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Ticket {

    private UUID id;

    private String reference;

    private String title;

    private String description;

    private TicketCategory category;

    private TicketPriority priority;

    private TicketStatus status;

    private String companyName;

    private String requesterSubject;

    private String requesterName;

    private String requesterEmail;

    private LocalDate requiredBy;

    private Instant respondBy;

    private Instant createdAt;

    private Instant updatedAt;

    /** Optimistic concurrency token; persistence adapters translate it to their native version field. */
    private Long revision;

    /** Moves the ticket to a new status when the transition is allowed. */
    public void changeStatus(TicketStatus nextStatus, Instant changedAt) {
        if (!status.canMoveTo(nextStatus)) {
            throw new InvalidStatusTransitionException(reference, status, nextStatus);
        }
        status = nextStatus;
        updatedAt = changedAt;
    }
}
