package com.gamer.fowever.tabletopserv.dto;

import com.gamer.fowever.tabletopserv.domain.EventType;

import java.time.LocalDateTime;

public record SessionEventDto(Long id, EventType type, Object payload, LocalDateTime createdAt) {
}