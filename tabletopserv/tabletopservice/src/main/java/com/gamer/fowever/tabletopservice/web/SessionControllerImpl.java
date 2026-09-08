package com.gamer.fowever.tabletopservice.web;

import com.gamer.fowever.tabletopapi.SessionApi;
import com.gamer.fowever.tabletopapi.dto.CreateSessionRequest;
import com.gamer.fowever.tabletopapi.dto.JoinSessionRequest;
import com.gamer.fowever.tabletopapi.dto.SessionSummary;
import com.gamer.fowever.tabletopapi.support.ApiException;
import com.gamer.fowever.tabletopservice.domain.User;
import com.gamer.fowever.tabletopservice.repository.ParticipantRepository;
import com.gamer.fowever.tabletopservice.service.SessionService;
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
public class SessionControllerImpl implements SessionApi {

    private final SessionService sessionService;
    private final ParticipantRepository participantRepository;

    public SessionControllerImpl(SessionService sessionService, ParticipantRepository participantRepository) {
        this.sessionService = sessionService;
        this.participantRepository = participantRepository;
    }

    @PostMapping
    @Override
    public ResponseEntity<SessionSummary> create(@Valid @RequestBody CreateSessionRequest request,
                                                 Authentication authentication) {
        User user = currentUser(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sessionService.createSession(user, request.name(), request.gameSlug()));
    }

    @GetMapping("/{id}")
    @Override
    public SessionSummary snapshot(@PathVariable Long id, Authentication authentication) {
        User user = currentUser(authentication);
        requireMember(id, user);
        return sessionService.getSnapshot(id);
    }

    @PostMapping("/join")
    @Override
    public SessionSummary join(@Valid @RequestBody JoinSessionRequest request, Authentication authentication) {
        return sessionService.joinSession(currentUser(authentication), request.inviteCode());
    }

    @PostMapping("/{id}/leave")
    @Override
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