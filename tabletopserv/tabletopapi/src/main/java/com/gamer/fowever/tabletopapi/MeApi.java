package com.gamer.fowever.tabletopapi;

import com.gamer.fowever.tabletopapi.dto.UserSummary;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/users")
public interface MeApi {

    @GetMapping("/me")
    UserSummary me(Authentication authentication);
}