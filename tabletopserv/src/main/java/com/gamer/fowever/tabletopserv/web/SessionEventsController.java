package com.gamer.fowever.tabletopserv.web;

import com.gamer.fowever.tabletopserv.domain.User;
import com.gamer.fowever.tabletopserv.dto.ChatMessage;
import com.gamer.fowever.tabletopserv.dto.SessionEventDto;
import com.gamer.fowever.tabletopserv.dto.SessionSummary;
import com.gamer.fowever.tabletopserv.service.SessionPresenceService;
import com.gamer.fowever.tabletopserv.service.SessionService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class SessionEventsController {

    private final SessionService sessionService;
    private final SessionPresenceService presenceService;

    public SessionEventsController(SessionService sessionService, SessionPresenceService presenceService) {
        this.sessionService = sessionService;
        this.presenceService = presenceService;
    }

    @MessageMapping("/sessions/{sessionId}/chat")
    @SendTo("/topic/sessions/{sessionId}")
    public SessionEventDto chat(@DestinationVariable Long sessionId,
                                @Payload ChatMessage message,
                                Principal principal) {
        User user = asUser(principal);
        return sessionService.addChatMessage(user, sessionId, message.text());
    }

    @SubscribeMapping("/sessions/{sessionId}")
    public SessionSummary subscribe(@DestinationVariable Long sessionId,
                                    Principal principal,
                                    @Header("simpSessionId") String simpSessionId) {
        User user = asUser(principal);
        presenceService.joined(sessionId, user, simpSessionId);
        return sessionService.getSnapshot(sessionId);
    }

    private User asUser(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof User user) {
            return user;
        }
        if (principal instanceof User user) {
            return user;
        }
        throw new AccessDeniedException("Authentication required");
    }
}