package uk.co.techsavvysolutions.tickets.identity.application;

/** A signed access token returned by an authentication use case. */
public record IssuedAccessToken(String accessToken, long expiresInSeconds) {}
