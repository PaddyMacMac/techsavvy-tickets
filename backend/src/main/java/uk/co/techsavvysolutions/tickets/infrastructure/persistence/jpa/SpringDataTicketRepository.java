package uk.co.techsavvysolutions.tickets.infrastructure.persistence.jpa;

import uk.co.techsavvysolutions.tickets.domain.ticket.TicketStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataTicketRepository extends JpaRepository<TicketEntity, UUID>, JpaSpecificationExecutor<TicketEntity> {

    @Override
    @EntityGraph(attributePaths = {"company", "requester"})
    Optional<TicketEntity> findOne(Specification<TicketEntity> specification);

    @Override
    @EntityGraph(attributePaths = {"company", "requester"})
    Page<TicketEntity> findAll(Specification<TicketEntity> specification, Pageable pageable);

    @Query("select t.status as status, count(t) as total from TicketEntity t group by t.status")
    List<StatusCount> countGroupedByStatus();

    @Query("""
            select t.status as status, count(t) as total
            from TicketEntity t
            where t.requester.subject = :subject
            group by t.status""")
    List<StatusCount> countGroupedByStatusForRequester(@Param("subject") String subject);

    interface StatusCount {

        TicketStatus getStatus();

        long getTotal();
    }
}
