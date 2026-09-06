package com.gamer.fowever.tabletopserv.dto;

public record ChatPayload(UserSummary sender, String text, String sentAt) {
}