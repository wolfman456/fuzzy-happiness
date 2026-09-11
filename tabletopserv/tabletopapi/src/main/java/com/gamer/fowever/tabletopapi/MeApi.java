package com.gamer.fowever.tabletopapi;

import com.gamer.fowever.tabletopapi.dto.ChangePasswordRequest;
import com.gamer.fowever.tabletopapi.dto.UpdateProfileRequest;
import com.gamer.fowever.tabletopapi.dto.UpdateUsernameRequest;
import com.gamer.fowever.tabletopapi.dto.UserSummary;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/users")
public interface MeApi {

    @GetMapping("/me")
    UserSummary me(Authentication authentication);

    @PatchMapping("/me/username")
    UserSummary updateUsername(@Valid @RequestBody UpdateUsernameRequest request,
                               Authentication authentication);

    @PatchMapping("/me/profile")
    UserSummary updateProfile(@Valid @RequestBody UpdateProfileRequest request,
                              Authentication authentication);

    @PatchMapping("/me/password")
    UserSummary changePassword(@Valid @RequestBody ChangePasswordRequest request,
                               Authentication authentication);
}