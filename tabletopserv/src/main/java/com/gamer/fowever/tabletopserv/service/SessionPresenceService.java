package com.gamer.fowever.tabletopserv.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.gamer.fowever.tabletopserv.domain.EventType;
import com.gamer.fowever.tabletopserv.domain.User;
import com.gamer.fowever.tabletopserv.dto.PresencePayload;
import com.gamer.fowever.tabletopserv.dto.SessionEventDto;
import com.gamer.fowever.tabletopserv.dto.UserSummary;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionPresenceService {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String TOPIC = "/topic/sessions/%d";

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, ConcurrentHashMap<Long, UserSummary>> presentBySimpSession =
            new ConcurrentHashMap<>();

    public SessionPresenceService(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    public void joined(Long sessionId, User user, String simpSessionId) {
        presentBySimpSession.computeIfAbsent(simpSessionId, key -> new ConcurrentHashMap<>())
                .put(sessionId, UserSummary.from(user));
        broadcast(sessionId, UserSummary.from(user), "joined");
    }

    public void left(String simpSessionId) {
        ConcurrentHashMap<Long, UserSummary> sessions = presentBySimpSession.remove(simpSessionId);
        if (sessions == null) {
            return;
        }
        sessions.forEach((sessionId, userSummary) -> broadcast(sessionId, userSummary, "left"));
    }

    Map<Long, UserSummary> presentForTesting(String simpSessionId) {
        return presentBySimpSession.get(simpSessionId);
    }

    private void broadcast(Long sessionId, UserSummary user, String action) {
        PresencePayload payload = new PresencePayload(user, action, LocalDateTime.now().format(TIMESTAMP));
        messagingTemplate.convertAndSend(TOPIC.formatted(sessionId),
                new SessionEventDto(null, EventType.PRESENCE, toJson(payload), LocalDateTime.now()));
    }

    private Object toJson(Object value) {
        try {
            return objectMapper.readTree(objectMapper.writeValueAsString(value));
        } catch (JacksonException e) {
            return value;
        }
    }
}