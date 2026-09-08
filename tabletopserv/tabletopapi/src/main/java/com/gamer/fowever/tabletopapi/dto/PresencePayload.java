package com.gamer.fowever.tabletopapi.dto;

public record PresencePayload(UserSummary sender, String action, String occurredAt) {
}