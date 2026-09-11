package com.gamer.fowever.tabletopservice.web;

import com.gamer.fowever.tabletopapi.MeApi;
import com.gamer.fowever.tabletopapi.dto.ChangePasswordRequest;
import com.gamer.fowever.tabletopapi.dto.UpdateProfileRequest;
import com.gamer.fowever.tabletopapi.dto.UpdateUsernameRequest;
import com.gamer.fowever.tabletopapi.dto.UserSummary;
import com.gamer.fowever.tabletopservice.domain.User;
import com.gamer.fowever.tabletopservice.service.AuthService;
import com.gamer.fowever.tabletopservice.support.Dtos;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class MeControllerImpl implements MeApi {

    private final AuthService authService;

    public MeControllerImpl(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    @Override
    public UserSummary me(Authentication authentication) {
        return Dtos.userSummary((User) authentication.getPrincipal());
    }

    @PatchMapping("/me/username")
    @Override
    public UserSummary updateUsername(@RequestBody UpdateUsernameRequest request,
                                     Authentication authentication) {
        return authService.updateUsername((User) authentication.getPrincipal(), request);
    }

    @PatchMapping("/me/profile")
    @Override
    public UserSummary updateProfile(@RequestBody UpdateProfileRequest request,
                                    Authentication authentication) {
        return authService.updateProfile((User) authentication.getPrincipal(), request);
    }

    @PatchMapping("/me/password")
    @Override
    public UserSummary changePassword(@RequestBody ChangePasswordRequest request,
                                     Authentication authentication) {
        return authService.changePassword((User) authentication.getPrincipal(), request);
    }
}