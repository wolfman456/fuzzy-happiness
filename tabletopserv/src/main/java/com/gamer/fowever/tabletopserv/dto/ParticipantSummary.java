package com.gamer.fowever.tabletopserv.dto;

import com.gamer.fowever.tabletopserv.domain.Role;

import java.time.LocalDateTime;

public record ParticipantSummary(UserSummary user, Role role, LocalDateTime joinedAt) {

    public static ParticipantSummary from(com.gamer.fowever.tabletopserv.domain.Participant participant) {
        return new ParticipantSummary(UserSummary.from(participant.getUser()), participant.getRole(), participant.getJoinedAt());
    }
}