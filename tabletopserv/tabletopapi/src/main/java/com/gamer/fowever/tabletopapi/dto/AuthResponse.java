package com.gamer.fowever.tabletopapi.dto;

public record AuthResponse(String token, String tokenType, long expiresInSeconds, UserSummary user) {
}