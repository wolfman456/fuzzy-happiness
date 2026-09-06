package com.gamer.fowever.tabletopserv.security;

import com.gamer.fowever.tabletopserv.domain.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public class AuthenticatedUser extends UsernamePasswordAuthenticationToken {

    private final User user;

    public AuthenticatedUser(User user) {
        super(user, null, user.getAuthorities());
        this.user = user;
    }

    @Override
    public String getName() {
        return user.getUsername();
    }
}