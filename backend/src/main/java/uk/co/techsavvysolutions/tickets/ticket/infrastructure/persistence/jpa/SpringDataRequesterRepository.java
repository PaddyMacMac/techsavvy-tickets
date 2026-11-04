package uk.co.techsavvysolutions.tickets.ticket.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataRequesterRepository extends JpaRepository<RequesterEntity, String> {}
