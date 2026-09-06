package com.gamer.fowever.tabletopserv.dto;

import com.gamer.fowever.tabletopserv.domain.SessionStatus;

import java.util.List;

public record SessionSummary(
        Long id,
        String name,
        String inviteCode,
        String gameSlug,
        String gameDisplayName,
        SessionStatus status,
        UserSummary createdBy,
        List<ParticipantSummary> participants,
        List<SessionEventDto> recentEvents) {
}