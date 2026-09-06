package com.gamer.fowever.tabletopserv.dto;

public record PresencePayload(UserSummary sender, String action, String occurredAt) {
}