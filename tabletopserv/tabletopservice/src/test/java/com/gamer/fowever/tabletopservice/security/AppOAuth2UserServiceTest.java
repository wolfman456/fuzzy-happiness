package com.gamer.fowever.tabletopservice.security;

import com.gamer.fowever.tabletopapi.support.ApiException;
import com.gamer.fowever.tabletopservice.domain.User;
import com.gamer.fowever.tabletopservice.service.SocialLoginService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppOAuth2UserServiceTest {

    @Mock
    private OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;
    @Mock
    private SocialLoginService socialLoginService;

    private static OAuth2UserRequest request(String registrationId) {
        ClientRegistration registration = ClientRegistration.withRegistrationId(registrationId)
                .clientId("id").clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("email").authorizationUri("https://example.com/auth")
                .tokenUri("https://example.com/token")
                .userNameAttributeName("sub").build();
        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "access-token",
                Instant.now(), Instant.now().plusSeconds(60));
        return new OAuth2UserRequest(registration, token);
    }

    private static OAuth2User loaded() {
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("sub", "google-1", "email", "aria@example.com", "email_verified", true),
                "sub");
    }

    @Test
    void wrapsProfileWithResolvedUserId() {
        User user = new User("aria", "Aria", "aria@example.com", null, "hash");
        user.setId(42L);
        when(delegate.loadUser(any())).thenReturn(loaded());
        when(socialLoginService.resolveLogin(anyString(), anyString(), anyString(), anyBoolean(), any(), any()))
                .thenReturn(user);

        OAuth2User result = new AppOAuth2UserService(delegate, socialLoginService).loadUser(request("google"));

        assertThat(result).isInstanceOf(AppOAuth2User.class);
        assertThat(((AppOAuth2User) result).userId()).isEqualTo(42L);
        assertThat(result.getName()).isEqualTo("42");
    }

    @Test
    void translatesApiExceptionIntoAuthenticationException() {
        when(delegate.loadUser(any())).thenReturn(loaded());
        when(socialLoginService.resolveLogin(anyString(), anyString(), anyString(), anyBoolean(), any(), any()))
                .thenThrow(ApiException.conflict("already linked"));

        AppOAuth2UserService service = new AppOAuth2UserService(delegate, socialLoginService);
        OAuth2UserRequest request = request("google");

        assertThatThrownBy(() -> service.loadUser(request))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("already linked")
                .extracting(ex -> ((OAuth2AuthenticationException) ex).getError().getErrorCode())
                .isEqualTo(AppOAuth2UserService.ERROR_CODE);
    }
}
