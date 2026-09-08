package com.gamer.fowever.tabletopapi.dto;

public record TurnCommandRequest(TurnAction action, Long tokenId) {
}