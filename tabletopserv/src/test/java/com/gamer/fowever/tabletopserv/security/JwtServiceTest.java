package com.gamer.fowever.tabletopserv.security;

import com.gamer.fowever.tabletopserv.domain.AuthRole;
import com.gamer.fowever.tabletopserv.domain.User;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET =
            "JtCeUaD41EV9iI+WoPojPt0Llx3L46tk4lSra9ttjo5a88nzHJMNLZZELKkN3Qgc";

    private final JwtService jwtService = new JwtService(SECRET, 60_000);

    private User user(long id, String username, AuthRole role) {
        User user = new User(username, "Name", username + "@example.com",
                LocalDate.of(1990, 1, 15), "Password1!");
        user.setId(id);
        user.setAuthRole(role);
        return user;
    }

    @Test
    void generatesParsableTokenWithClaims() {
        User user = user(42L, "aria", AuthRole.MODERATOR);

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
        assertThat(jwtService.extractRole(token)).isEqualTo("MODERATOR");
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void rejectsTokenForAnotherUser() {
        User owner = user(1L, "aria", AuthRole.USER);
        User other = user(2L, "ivo", AuthRole.USER);
        String token = jwtService.generateToken(owner);

        assertThat(jwtService.isTokenValid(token, other)).isFalse();
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtService.generateToken(user(1L, "aria", AuthRole.USER));
        String tampered = token.substring(0, token.length() - 4) + "AAAA";

        assertThatThrownBy(() -> jwtService.extractUserId(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredToken() {
        JwtService shortLived = new JwtService(SECRET, 1);
        String token = shortLived.generateToken(user(1L, "aria", AuthRole.USER));

        assertThatThrownBy(() -> jwtService.isTokenValid(token, user(1L, "aria", AuthRole.USER)))
                .isInstanceOf(JwtException.class);
    }
}