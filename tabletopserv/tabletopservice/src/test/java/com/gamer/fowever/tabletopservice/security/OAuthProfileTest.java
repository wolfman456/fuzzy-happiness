package com.gamer.fowever.tabletopservice.security;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthProfileTest {

    @Test
    void extractsGoogleClaimsAndVerification() {
        Map<String, Object> attributes = Map.of(
                "sub", "google-123",
                "email", "aria@example.com",
                "email_verified", true,
                "name", "Aria",
                "picture", "https://img/aria.png");

        OAuthProfile profile = OAuthProfile.extract("google", attributes);

        assertThat(profile).isEqualTo(new OAuthProfile(
                "google-123", "aria@example.com", true, "Aria", "https://img/aria.png"));
    }

    @Test
    void treatsStringVerificationFlagAsTrue() {
        OAuthProfile profile = OAuthProfile.extract("google", Map.of(
                "sub", "1", "email_verified", "TRUE"));

        assertThat(profile.emailVerified()).isTrue();
    }

    @Test
    void githubUsesLoginAsFallbackNameAndNeverAssertsVerification() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 44);
        attributes.put("email", "octo@example.com");
        attributes.put("login", "octocat");
        attributes.put("avatar_url", "https://img/octo.png");

        OAuthProfile profile = OAuthProfile.extract("github", attributes);

        assertThat(profile.subject()).isEqualTo("44");
        assertThat(profile.emailVerified()).isFalse();
        assertThat(profile.displayName()).isEqualTo("octocat");
        assertThat(profile.avatarUrl()).isEqualTo("https://img/octo.png");
    }

    @Test
    void githubPrefersNameOverLogin() {
        OAuthProfile profile = OAuthProfile.extract("github", Map.of(
                "id", "44", "name", "Mona", "login", "octocat"));

        assertThat(profile.displayName()).isEqualTo("Mona");
    }

    @Test
    void facebookTreatsEmailAsVerifiedAndUnwrapsNestedPicture() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", "fb-9");
        attributes.put("email", "fb@example.com");
        attributes.put("name", "FB User");
        attributes.put("picture", Map.of("data", Map.of("url", "https://img/fb.png")));

        OAuthProfile profile = OAuthProfile.extract("facebook", attributes);

        assertThat(profile.subject()).isEqualTo("fb-9");
        assertThat(profile.emailVerified()).isTrue();
        assertThat(profile.avatarUrl()).isEqualTo("https://img/fb.png");
    }

    @Test
    void facebookSupportsFlatPictureMap() {
        OAuthProfile profile = OAuthProfile.extract("facebook", Map.of(
                "id", "fb-9", "picture", Map.of("url", "https://img/flat.png")));

        assertThat(profile.avatarUrl()).isEqualTo("https://img/flat.png");
    }

    @Test
    void unknownProviderFallsBackToOidcClaims() {
        OAuthProfile profile = OAuthProfile.extract("okta", Map.of(
                "sub", "oidc-1",
                "email", "oidc@example.com",
                "preferred_username", "oidcuser",
                "avatar_url", "https://img/oidc.png"));

        assertThat(profile.subject()).isEqualTo("oidc-1");
        assertThat(profile.displayName()).isEqualTo("oidcuser");
        assertThat(profile.avatarUrl()).isEqualTo("https://img/oidc.png");
    }

    @Test
    void unknownProviderFallsBackToIdWhenNoSubject() {
        OAuthProfile profile = OAuthProfile.extract("okta", Map.of("id", "id-only"));

        assertThat(profile.subject()).isEqualTo("id-only");
    }

    @Test
    void handlesNullRegistrationIdAndMissingAttributes() {
        OAuthProfile profile = OAuthProfile.extract(null, Map.of());

        assertThat(profile.subject()).isNull();
        assertThat(profile.email()).isNull();
        assertThat(profile.emailVerified()).isFalse();
        assertThat(profile.displayName()).isNull();
        assertThat(profile.avatarUrl()).isNull();
    }

    @Test
    void facebookPictureAsPlainStringIsPassedThrough() {
        OAuthProfile profile = OAuthProfile.extract("facebook", Map.of(
                "id", "fb-9", "picture", "https://img/direct.png"));

        assertThat(profile.avatarUrl()).isEqualTo("https://img/direct.png");
    }
}
