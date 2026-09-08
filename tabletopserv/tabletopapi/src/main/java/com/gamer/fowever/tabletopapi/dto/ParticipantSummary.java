package com.gamer.fowever.tabletopapi.dto;

import com.gamer.fowever.tabletopapi.Role;

import java.time.LocalDateTime;

public record ParticipantSummary(UserSummary user, Role role, LocalDateTime joinedAt) {
}