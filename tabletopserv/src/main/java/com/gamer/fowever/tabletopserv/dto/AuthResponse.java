package com.gamer.fowever.tabletopserv.dto;

public record AuthResponse(String token, String tokenType, long expiresInSeconds, UserSummary user) {
}