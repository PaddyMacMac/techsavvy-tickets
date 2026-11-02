package uk.co.techsavvysolutions.tickets.api.ticket;

import static uk.co.techsavvysolutions.tickets.TicketFixtures.aTicket;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import uk.co.techsavvysolutions.tickets.application.ticket.ChangeTicketStatusService;
import uk.co.techsavvysolutions.tickets.application.ticket.LogTicketService;
import uk.co.techsavvysolutions.tickets.application.ticket.TicketQueryService;
import uk.co.techsavvysolutions.tickets.application.ticket.TicketSummaryService;
import uk.co.techsavvysolutions.tickets.configuration.PortalProperties;
import uk.co.techsavvysolutions.tickets.domain.ticket.Paged;
import uk.co.techsavvysolutions.tickets.domain.ticket.TicketStatus;
import uk.co.techsavvysolutions.tickets.infrastructure.security.configuration.ApiSecurityConfiguration;
import uk.co.techsavvysolutions.tickets.infrastructure.security.identity.JwtCurrentUserMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(TicketController.class)
@Import({ApiSecurityConfiguration.class, JwtCurrentUserMapper.class, LogTicketRequestMapper.class, TicketResponseMapper.class})
class TicketControllerTest {

    @TestConfiguration
    @EnableConfigurationProperties(PortalProperties.class)
    static class PropertiesConfig {}

    @Autowired MockMvc mvc;
    @MockitoBean LogTicketService logTicketService;
    @MockitoBean ChangeTicketStatusService changeTicketStatusService;
    @MockitoBean TicketQueryService ticketQueries;
    @MockitoBean TicketSummaryService ticketSummaries;
    @MockitoBean JwtDecoder jwtDecoder;

    private static RequestPostProcessor signedInAs(String role) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role))
                .jwt(token -> token.subject("user-1").claim("name", "Test User").claim("roles", List.of(role)));
    }

    private static final String VALID_BODY = """
            {"title":"New starter laptop","description":"Laptop for Alex, starts Monday.",
             "category":"NEW_STARTER","priority":"NORMAL","companyName":"Acme Bury Ltd"}""";

    @Test
    void rejectsCallsWithoutAToken() throws Exception {
        mvc.perform(get("/api/tickets")).andExpect(status().isUnauthorized());
    }

    @Test
    void clientsCanListTickets() throws Exception {
        when(ticketQueries.listTickets(any(), any())).thenReturn(new Paged<>(List.of(aTicket().build()), 0, 20, 1));

        mvc.perform(get("/api/tickets").with(signedInAs("CLIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("New starter laptop"))
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void loggingATicketReturnsCreatedWithLocation() throws Exception {
        var ticket = aTicket().build();
        when(logTicketService.logTicket(any())).thenReturn(ticket);

        mvc.perform(post("/api/tickets").with(signedInAs("CLIENT")).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/tickets/" + ticket.getId()))
                .andExpect(jsonPath("$.reference").value(ticket.getReference()));
    }

    @Test
    void invalidTicketsGetFieldLevelErrors() throws Exception {
        var body = """
                {"title":"","description":"x","category":"HARDWARE","priority":"LOW","companyName":"Acme"}""";

        mvc.perform(post("/api/tickets").with(signedInAs("CLIENT")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    void clientsCannotChangeStatus() throws Exception {
        mvc.perform(patch("/api/tickets/{id}/status", aTicket().build().getId())
                        .with(signedInAs("CLIENT")).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void engineersCanChangeStatus() throws Exception {
        var ticket = aTicket().build();
        ticket.changeStatus(TicketStatus.IN_PROGRESS, ticket.getUpdatedAt());
        when(changeTicketStatusService.changeTicketStatus(eq(ticket.getId()), eq(TicketStatus.IN_PROGRESS))).thenReturn(ticket);

        mvc.perform(patch("/api/tickets/{id}/status", ticket.getId())
                        .with(signedInAs("ENGINEER")).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }
}
