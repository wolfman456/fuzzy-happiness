package com.gamer.fowever.tabletopservice.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

/**
 * Wraps the provider's {@link OAuth2User} while carrying the resolved local user id, so the
 * success handler can mint our own JWT without a second identity lookup.
 */
public record AppOAuth2User(Collection<? extends GrantedAuthority> authorities,
                            Map<String, Object> attributes,
                            Long userId) implements OAuth2User {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
