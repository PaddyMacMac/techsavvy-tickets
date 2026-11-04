package uk.co.techsavvysolutions.tickets;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class TicketsModularityTest {

    @Test
    void verifiesModuleBoundaries() {
        ApplicationModules.of(TicketsApplication.class).verify();
    }
}
