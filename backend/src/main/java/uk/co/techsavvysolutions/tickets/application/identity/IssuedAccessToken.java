package uk.co.techsavvysolutions.tickets.application.identity;

/** A signed access token returned by an authentication use case. */
public record IssuedAccessToken(String accessToken, long expiresInSeconds) {}
