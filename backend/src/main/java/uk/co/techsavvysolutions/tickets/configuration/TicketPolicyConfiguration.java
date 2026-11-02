package uk.co.techsavvysolutions.tickets.configuration;

import uk.co.techsavvysolutions.tickets.domain.ticket.DefaultSlaPolicy;
import uk.co.techsavvysolutions.tickets.domain.ticket.SlaPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class TicketPolicyConfiguration {

    @Bean
    SlaPolicy ticketSlaPolicy() {
        return new DefaultSlaPolicy();
    }
}
