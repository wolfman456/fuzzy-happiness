package com.gamer.fowever.tabletopapi;

import com.gamer.fowever.tabletopapi.dto.RollRequest;
import com.gamer.fowever.tabletopapi.dto.SessionEventDto;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/sessions/{sessionId}/roll")
public interface DiceApi {

    @PostMapping
    SessionEventDto roll(@PathVariable Long sessionId,
                         @Valid @RequestBody RollRequest request,
                         Authentication authentication);
}