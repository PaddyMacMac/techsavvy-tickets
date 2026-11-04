package uk.co.techsavvysolutions.tickets.ticket.infrastructure.persistence.jpa;

import uk.co.techsavvysolutions.tickets.ticket.domain.Ticket;
import org.springframework.stereotype.Component;

@Component
class TicketEntityMapper {

    Ticket toDomain(TicketEntity entity) {
        return Ticket.builder()
                .id(entity.getId())
                .reference(entity.getReference())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .priority(entity.getPriority())
                .status(entity.getStatus())
                .companyName(entity.getCompany().getName())
                .requesterSubject(entity.getRequester().getSubject())
                .requesterName(entity.getRequester().getName())
                .requesterEmail(entity.getRequester().getEmail())
                .requiredBy(entity.getRequiredBy())
                .respondBy(entity.getRespondBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .revision(entity.getVersion())
                .build();
    }

    TicketEntity toEntity(Ticket ticket, CompanyEntity company, RequesterEntity requester) {
        return TicketEntity.builder()
                .id(ticket.getId())
                .reference(ticket.getReference())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .category(ticket.getCategory())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .company(company)
                .requester(requester)
                .requiredBy(ticket.getRequiredBy())
                .respondBy(ticket.getRespondBy())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .version(ticket.getRevision())
                .build();
    }
}
