package com.gamer.fowever.tabletopserv.config;

import com.gamer.fowever.tabletopserv.service.SessionPresenceService;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener implements ApplicationListener<SessionDisconnectEvent> {

    private final SessionPresenceService presenceService;

    public WebSocketEventListener(SessionPresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @Override
    public void onApplicationEvent(SessionDisconnectEvent event) {
        presenceService.left(event.getSessionId());
    }
}