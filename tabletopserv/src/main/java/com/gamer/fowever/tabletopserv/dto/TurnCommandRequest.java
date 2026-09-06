package com.gamer.fowever.tabletopserv.dto;

public record TurnCommandRequest(TurnAction action, Long tokenId) {
}