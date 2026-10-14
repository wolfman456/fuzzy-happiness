package com.gamer.fowever.tabletopservice.security;

import com.gamer.fowever.tabletopservice.domain.User;
import com.gamer.fowever.tabletopservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OAuth2LoginSuccessHandlerTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final UserRepository userRepository = mock(UserRepository.class);

    private OAuth2LoginSuccessHandler handler() {
        return new OAuth2LoginSuccessHandler(jwtService, userRepository, "http://frontend.test", 3_600_000L);
    }

    private static OAuth2AuthenticationToken authentication(long userId) {
        AppOAuth2User principal = new AppOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")), Map.of("sub", "s"), userId);
        return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");
    }

    @Test
    void redirectsToSpaWithMintedTokenInFragment() throws Exception {
        User user = new User("aria", "Aria", "aria@example.com", null, "hash");
        user.setId(42L);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("signed.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler().onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication(42L));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://frontend.test/oauth/callback#token=signed.jwt.token&tokenType=Bearer&expiresInSeconds=3600");
    }

    @Test
    void failsLoudlyWhenUserDisappeared() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> handler().onAuthenticationSuccess(
                new MockHttpServletRequest(), response, authentication(42L)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(response.getRedirectedUrl()).isNull();
    }
}
