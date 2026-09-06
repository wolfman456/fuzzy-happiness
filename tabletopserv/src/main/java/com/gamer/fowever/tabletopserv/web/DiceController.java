package com.gamer.fowever.tabletopserv.web;

import com.gamer.fowever.tabletopserv.domain.User;
import com.gamer.fowever.tabletopserv.dto.RollRequest;
import com.gamer.fowever.tabletopserv.dto.SessionEventDto;
import com.gamer.fowever.tabletopserv.service.DiceService;
import jakarta.validation.Valid;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions/{sessionId}/roll")
public class DiceController {

    private static final String TOPIC = "/topic/sessions/%d";

    private final DiceService diceService;
    private final SimpMessagingTemplate messagingTemplate;

    public DiceController(DiceService diceService, SimpMessagingTemplate messagingTemplate) {
        this.diceService = diceService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping
    public SessionEventDto roll(@PathVariable Long sessionId,
                                @Valid @RequestBody RollRequest request,
                                Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        DiceService.DiceRoll roll = diceService.roll(user, sessionId, request);
        messagingTemplate.convertAndSend(TOPIC.formatted(sessionId), roll.topicEvent());
        if (roll.isHidden()) {
            messagingTemplate.convertAndSendToUser(user.getUsername(), "/queue/dice", roll.event());
        }
        return roll.event();
    }
}