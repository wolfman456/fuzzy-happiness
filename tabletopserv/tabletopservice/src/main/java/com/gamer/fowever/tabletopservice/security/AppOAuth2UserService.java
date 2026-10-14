package com.gamer.fowever.tabletopservice.security;

import com.gamer.fowever.tabletopapi.support.ApiException;
import com.gamer.fowever.tabletopservice.domain.User;
import com.gamer.fowever.tabletopservice.service.SocialLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

/**
 * Loads the provider profile, then links or creates the local account before the OAuth2
 * authentication is completed. The failure surfaces as an {@link OAuth2AuthenticationException}
 * so the failure handler can bounce the browser back to the frontend with a friendly code.
 */
@Component
public class AppOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    static final String ERROR_CODE = "social_login_failed";

    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;
    private final SocialLoginService socialLoginService;

    @Autowired
    public AppOAuth2UserService(SocialLoginService socialLoginService) {
        this(new DefaultOAuth2UserService(), socialLoginService);
    }

    AppOAuth2UserService(OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate,
                         SocialLoginService socialLoginService) {
        this.delegate = delegate;
        this.socialLoginService = socialLoginService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User loaded = delegate.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();
        OAuthProfile profile = OAuthProfile.extract(provider, loaded.getAttributes());
        try {
            User user = socialLoginService.resolveLogin(provider, profile.subject(), profile.email(),
                    profile.emailVerified(), profile.displayName(), profile.avatarUrl());
            return new AppOAuth2User(loaded.getAuthorities(), loaded.getAttributes(), user.getId());
        } catch (ApiException ex) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(ERROR_CODE, ex.getMessage(), null), ex);
        }
    }
}
