package com.gamer.fowever.tabletopapi.dto;

public record ChatPayload(UserSummary sender, String text, String sentAt) {
}