package com.gamer.fowever.tabletopapi;

import com.gamer.fowever.tabletopapi.dto.CreateSessionRequest;
import com.gamer.fowever.tabletopapi.dto.JoinSessionRequest;
import com.gamer.fowever.tabletopapi.dto.SessionSummary;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/sessions")
public interface SessionApi {

    @PostMapping
    ResponseEntity<SessionSummary> create(@Valid @RequestBody CreateSessionRequest request, Authentication authentication);

    @GetMapping("/{id}")
    SessionSummary snapshot(@PathVariable Long id, Authentication authentication);

    @PostMapping("/join")
    SessionSummary join(@Valid @RequestBody JoinSessionRequest request, Authentication authentication);

    @PostMapping("/{id}/leave")
    ResponseEntity<String> leave(@PathVariable Long id, Authentication authentication);
}