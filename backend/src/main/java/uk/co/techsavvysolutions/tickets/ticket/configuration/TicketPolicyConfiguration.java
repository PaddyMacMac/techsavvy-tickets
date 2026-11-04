package uk.co.techsavvysolutions.tickets.ticket.configuration;

import uk.co.techsavvysolutions.tickets.ticket.domain.DefaultSlaPolicy;
import uk.co.techsavvysolutions.tickets.ticket.domain.SlaPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class TicketPolicyConfiguration {

    @Bean
    SlaPolicy ticketSlaPolicy() {
        return new DefaultSlaPolicy();
    }
}
