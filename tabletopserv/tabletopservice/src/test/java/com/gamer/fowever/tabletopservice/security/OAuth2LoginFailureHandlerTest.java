package com.gamer.fowever.tabletopservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2LoginFailureHandlerTest {

    private final OAuth2LoginFailureHandler handler =
            new OAuth2LoginFailureHandler("http://frontend.test");

    @Test
    void redirectsWithProviderErrorCodeAndMessage() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthenticationException exception = new OAuth2AuthenticationException(
                new OAuth2Error("social_login_failed", "This email is already linked.", null));

        handler.onAuthenticationFailure(new MockHttpServletRequest(), response, exception);

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://frontend.test/login?oauthError=social_login_failed"
                        + "&oauthMessage=This+email+is+already+linked.");
    }

    @Test
    void fallsBackToGenericCodeForNonOAuthFailures() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(new MockHttpServletRequest(), response,
                new BadCredentialsException("nope"));

        assertThat(response.getRedirectedUrl())
                .startsWith("http://frontend.test/login?oauthError=oauth_failed&oauthMessage=");
    }

    @Test
    void fallsBackToGenericMessageWhenDescriptionMissing() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthenticationException exception = new OAuth2AuthenticationException(new OAuth2Error("access_denied"));

        handler.onAuthenticationFailure(new MockHttpServletRequest(), response, exception);

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://frontend.test/login?oauthError=access_denied"
                        + "&oauthMessage=Sign-in+with+your+provider+failed.+Please+try+again.");
    }
}
