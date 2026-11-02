package uk.co.techsavvysolutions.tickets.infrastructure.persistence.jpa;

import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCompanyRepository extends JpaRepository<CompanyEntity, UUID> {

    Optional<CompanyEntity> findByName(String name);
}
