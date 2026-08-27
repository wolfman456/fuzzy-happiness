package com.gamer.fowever.tabletopserv.web;

import com.gamer.fowever.tabletopserv.domain.User;
import com.gamer.fowever.tabletopserv.dto.UserSummary;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class MeController {

    @GetMapping("/me")
    public UserSummary me(Authentication authentication) {
        return UserSummary.from((User) authentication.getPrincipal());
    }
}