package com.gamer.fowever.tabletopapi.dto;

/**
 * A configured social-login provider exposed to the frontend so it can render a sign-in button.
 * {@code authorizationPath} is the backend-relative OAuth2 authorization entry point
 * (e.g. {@code /oauth2/authorization/google}); the client prepends its API base URL.
 */
public record AuthProvider(String id, String label, String authorizationPath) {
}
