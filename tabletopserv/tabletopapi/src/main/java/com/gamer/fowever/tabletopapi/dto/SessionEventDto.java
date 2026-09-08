package com.gamer.fowever.tabletopapi.dto;

import com.gamer.fowever.tabletopapi.EventType;

import java.time.LocalDateTime;

public record SessionEventDto(Long id, EventType type, Object payload, LocalDateTime createdAt) {
}