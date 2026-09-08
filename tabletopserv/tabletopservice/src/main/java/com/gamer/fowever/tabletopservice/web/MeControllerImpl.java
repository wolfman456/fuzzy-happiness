package com.gamer.fowever.tabletopservice.web;

import com.gamer.fowever.tabletopapi.MeApi;
import com.gamer.fowever.tabletopapi.dto.UserSummary;
import com.gamer.fowever.tabletopservice.domain.User;
import com.gamer.fowever.tabletopservice.support.Dtos;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class MeControllerImpl implements MeApi {

    @GetMapping("/me")
    @Override
    public UserSummary me(Authentication authentication) {
        return Dtos.userSummary((User) authentication.getPrincipal());
    }
}