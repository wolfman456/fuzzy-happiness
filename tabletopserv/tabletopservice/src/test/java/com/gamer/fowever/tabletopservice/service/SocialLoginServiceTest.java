package com.gamer.fowever.tabletopservice.service;

import com.gamer.fowever.tabletopapi.dto.AuthProvider;
import com.gamer.fowever.tabletopapi.support.ApiException;
import com.gamer.fowever.tabletopservice.domain.User;
import com.gamer.fowever.tabletopservice.repository.UserRepository;
import com.gamer.fowever.tabletopservice.support.PiiCrypto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocialLoginServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ObjectProvider<ClientRegistrationRepository> registrationRepository;

    private SocialLoginService service;

    @BeforeEach
    void setUp() {
        service = new SocialLoginService(userRepository, passwordEncoder, registrationRepository);
    }

    private static ClientRegistration registration(String id) {
        return ClientRegistration.withRegistrationId(id)
                .clientId("client-id").clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("email").authorizationUri("https://example.com/auth")
                .tokenUri("https://example.com/token")
                .userInfoUri("https://example.com/userinfo")
                .userNameAttributeName("sub").clientName(id).build();
    }

    @Test
    void providersAreEmptyWithoutRegistrations() {
        when(registrationRepository.getIfAvailable()).thenReturn(null);
        assertThat(service.providers()).isEmpty();
    }

    @Test
    void providersAreLabelledAndOrdered() {
        ClientRegistrationRepository repository = new InMemoryClientRegistrationRepository(
                registration("facebook"), registration("github"), registration("google"), registration("custom"));
        when(registrationRepository.getIfAvailable()).thenReturn(repository);

        List<AuthProvider> providers = service.providers();

        assertThat(providers).extracting(AuthProvider::id)
                .containsExactly("google", "github", "facebook", "custom");
        assertThat(providers.get(0))
                .isEqualTo(new AuthProvider("google", "Google", "/oauth2/authorization/google"));
        assertThat(providers.get(3).label()).isEqualTo("Custom");
    }

    private User user(long id, String username, String email, String provider) {
        User user = new User(username, "A User", email, LocalDate.of(1990, 1, 1), "hash");
        user.setId(id);
        user.setOauthProvider(provider);
        return user;
    }

    @Test
    void reusesKnownProviderIdentityAndRefreshesAvatar() {
        User user = user(7L, "aria", "aria@example.com", "google");
        when(userRepository.findByOauthProviderAndOauthSubject("google", "sub-1")).thenReturn(Optional.of(user));

        User resolved = service.resolveLogin("google", "sub-1", "aria@example.com", true, "Aria", "https://img/a.png");

        assertThat(resolved).isSameAs(user);
        assertThat(user.getAvatar()).isEqualTo("https://img/a.png");
        verify(userRepository).save(user);
    }

    @Test
    void knownIdentityWithBlankAvatarDoesNotPersist() {
        User user = user(7L, "aria", "aria@example.com", "google");
        when(userRepository.findByOauthProviderAndOauthSubject("google", "sub-1")).thenReturn(Optional.of(user));

        service.resolveLogin("google", "sub-1", "aria@example.com", true, "Aria", null);

        verify(userRepository, never()).save(any());
    }

    @Test
    void linksVerifiedEmailToExistingPasswordAccount() {
        User user = user(3L, "local", "local@example.com", null);
        when(userRepository.findByOauthProviderAndOauthSubject("github", "gh-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmailKey(PiiCrypto.emailKey("local@example.com"))).thenReturn(Optional.of(user));
        user.setEmailVerified(false);

        User resolved = service.resolveLogin("github", "gh-1", "local@example.com", true, "Local", null);

        assertThat(resolved.getOauthProvider()).isEqualTo("github");
        assertThat(resolved.getOauthSubject()).isEqualTo("gh-1");
        assertThat(resolved.isEmailVerified()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void refusesToLinkUnverifiedEmailToExistingAccount() {
        User user = user(3L, "local", "local@example.com", null);
        when(userRepository.findByOauthProviderAndOauthSubject("github", "gh-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmailKey(PiiCrypto.emailKey("local@example.com"))).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.resolveLogin("github", "gh-1", "local@example.com", false, "Local", null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already exists");
        verify(userRepository, never()).save(any());
    }

    @Test
    void refusesToRelinkToADifferentProvider() {
        User user = user(3L, "local", "local@example.com", "google");
        when(userRepository.findByOauthProviderAndOauthSubject("github", "gh-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmailKey(PiiCrypto.emailKey("local@example.com"))).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.resolveLogin("github", "gh-1", "local@example.com", true, "Local", null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("different sign-in provider");
    }

    @Test
    void createsNewAccountWhenNoMatch() {
        when(userRepository.findByOauthProviderAndOauthSubject("google", "sub-9")).thenReturn(Optional.empty());
        when(userRepository.findByEmailKey(PiiCrypto.emailKey("new.person@example.com"))).thenReturn(Optional.empty());
        when(userRepository.existsByUsernameIgnoreCase("newperson")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("random-hash");

        User resolved = service.resolveLogin("google", "sub-9", "new.person@example.com", true,
                "New Person", "https://img/new.png");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("newperson");
        assertThat(saved.getDisplayName()).isEqualTo("New Person");
        assertThat(saved.getEmail()).isEqualTo("new.person@example.com");
        assertThat(saved.getDateOfBirth()).isNull();
        assertThat(saved.isEmailVerified()).isTrue();
        assertThat(saved.getOauthProvider()).isEqualTo("google");
        assertThat(saved.getOauthSubject()).isEqualTo("sub-9");
        assertThat(saved.getAvatar()).isEqualTo("https://img/new.png");
        assertThat(saved.getPasswordHash()).isEqualTo("random-hash");
        assertThat(resolved).isSameAs(saved);
    }

    @Test
    void createsAccountWithGeneratedNameWhenDisplayNameMissing() {
        when(userRepository.findByOauthProviderAndOauthSubject("github", "gh-2")).thenReturn(Optional.empty());
        when(userRepository.findByEmailKey(PiiCrypto.emailKey("solo@example.com"))).thenReturn(Optional.empty());
        when(userRepository.existsByUsernameIgnoreCase("solo")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("random-hash");

        User resolved = service.resolveLogin("github", "gh-2", "solo@example.com", false, "   ", null);

        assertThat(resolved.getUsername()).isEqualTo("solo");
        assertThat(resolved.getDisplayName()).isEqualTo("solo");
        assertThat(resolved.getAvatar()).isNull();
    }

    @Test
    void suffixesUsernameUntilFree() {
        when(userRepository.findByOauthProviderAndOauthSubject("google", "sub-3")).thenReturn(Optional.empty());
        when(userRepository.findByEmailKey(PiiCrypto.emailKey("taken@example.com"))).thenReturn(Optional.empty());
        when(userRepository.existsByUsernameIgnoreCase("taken")).thenReturn(true);
        when(userRepository.existsByUsernameIgnoreCase("taken-1")).thenReturn(true);
        when(userRepository.existsByUsernameIgnoreCase("taken-2")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("random-hash");

        User resolved = service.resolveLogin("google", "sub-3", "taken@example.com", true, "Taken", null);

        assertThat(resolved.getUsername()).isEqualTo("taken-2");
    }

    @Test
    void fallsBackToPlayerWhenEmailLocalPartTooShort() {
        when(userRepository.findByOauthProviderAndOauthSubject("google", "sub-4")).thenReturn(Optional.empty());
        when(userRepository.findByEmailKey(PiiCrypto.emailKey("a@example.com"))).thenReturn(Optional.empty());
        when(userRepository.existsByUsernameIgnoreCase("player")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("random-hash");

        User resolved = service.resolveLogin("google", "sub-4", "a@example.com", true, "Al", null);

        assertThat(resolved.getUsername()).isEqualTo("player");
    }

    @Test
    void rejectsProfilesWithoutEmail() {
        when(userRepository.findByOauthProviderAndOauthSubject("github", "gh-3")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveLogin("github", "gh-3", " ", false, "NoMail", null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("did not share an email");
    }

    @Test
    void labelHandlesSingleCharacterRegistrationId() {
        ClientRegistrationRepository repository =
                new InMemoryClientRegistrationRepository(registration("x"));
        when(registrationRepository.getIfAvailable()).thenReturn(repository);

        assertThat(service.providers().get(0).label()).isEqualTo("X");
    }
}
