package com.gamer.fowever.tabletopserv.web;

import com.gamer.fowever.tabletopserv.domain.User;
import com.gamer.fowever.tabletopserv.dto.CreateSessionRequest;
import com.gamer.fowever.tabletopserv.dto.JoinSessionRequest;
import com.gamer.fowever.tabletopserv.dto.SessionSummary;
import com.gamer.fowever.tabletopserv.repository.ParticipantRepository;
import com.gamer.fowever.tabletopserv.service.SessionService;
import com.gamer.fowever.tabletopserv.support.ApiException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;
    private final ParticipantRepository participantRepository;

    public SessionController(SessionService sessionService, ParticipantRepository participantRepository) {
        this.sessionService = sessionService;
        this.participantRepository = participantRepository;
    }

    @PostMapping
    public ResponseEntity<SessionSummary> create(@Valid @RequestBody CreateSessionRequest request,
                                                 Authentication authentication) {
        User user = currentUser(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sessionService.createSession(user, request.name(), request.gameSlug()));
    }

    @GetMapping("/{id}")
    public SessionSummary snapshot(@PathVariable Long id, Authentication authentication) {
        User user = currentUser(authentication);
        requireMember(id, user);
        return sessionService.getSnapshot(id);
    }

    @PostMapping("/join")
    public SessionSummary join(@Valid @RequestBody JoinSessionRequest request, Authentication authentication) {
        return sessionService.joinSession(currentUser(authentication), request.inviteCode());
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<String> leave(@PathVariable Long id, Authentication authentication) {
        sessionService.leaveSession(currentUser(authentication), id);
        return ResponseEntity.accepted().body("You have left the session");
    }

    private void requireMember(Long sessionId, User user) {
        if (!participantRepository.existsBySessionIdAndUserId(sessionId, user.getId())) {
            throw ApiException.forbidden("You are not a participant of this session");
        }
    }

    private User currentUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }
}