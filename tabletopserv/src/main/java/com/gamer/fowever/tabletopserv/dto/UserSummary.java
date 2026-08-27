package com.gamer.fowever.tabletopserv.dto;

import com.gamer.fowever.tabletopserv.domain.AuthRole;
import com.gamer.fowever.tabletopserv.domain.User;

public record UserSummary(Long id, String username, String displayName, String email,
                          AuthRole role, boolean emailVerified) {

    public static UserSummary from(User user) {
        return new UserSummary(user.getId(), user.getUsername(), user.getDisplayName(),
                user.getEmail(), user.getAuthRole(), user.isEmailVerified());
    }
}