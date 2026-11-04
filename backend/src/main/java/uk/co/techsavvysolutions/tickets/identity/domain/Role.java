package uk.co.techsavvysolutions.tickets.identity.domain;

import java.util.Arrays;
import java.util.Optional;

/** Roles a person can hold. Values match the app role values configured in Microsoft Entra ID. */
public enum Role {
    ENGINEER,
    CLIENT;

    public static Optional<Role> parse(String value) {
        return Arrays.stream(values()).filter(role -> role.name().equalsIgnoreCase(value)).findFirst();
    }
}
