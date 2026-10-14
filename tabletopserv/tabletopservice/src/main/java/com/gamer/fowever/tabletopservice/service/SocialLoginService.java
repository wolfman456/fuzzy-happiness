package com.gamer.fowever.tabletopservice.service;

import com.gamer.fowever.tabletopapi.AuthRole;
import com.gamer.fowever.tabletopapi.dto.AuthProvider;
import com.gamer.fowever.tabletopapi.support.ApiException;
import com.gamer.fowever.tabletopservice.domain.User;
import com.gamer.fowever.tabletopservice.repository.UserRepository;
import com.gamer.fowever.tabletopservice.support.PiiCrypto;
import com.gamer.fowever.tabletopservice.security.ClientRegistrations;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Social-login account resolution (R19). The OAuth2 handshake itself is owned by Spring Security;
 * this service turns a validated provider profile into a local {@link User}: it reuses an existing
 * provider identity, links by provider-verified email, or creates a fresh account.
 */
@Service
public class SocialLoginService {

    private static final Map<String, String> LABELS = Map.of(
            "google", "Google",
            "github", "GitHub",
            "facebook", "Facebook");
    private static final List<String> PREFERRED_ORDER = List.of("google", "github", "facebook");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectProvider<ClientRegistrationRepository> registrationRepository;

    public SocialLoginService(UserRepository userRepository,
                              PasswordEncoder passwordEncoder,
                              ObjectProvider<ClientRegistrationRepository> registrationRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.registrationRepository = registrationRepository;
    }

    /** The configured providers, in a stable order. Empty when no provider credentials exist. */
    public List<AuthProvider> providers() {
        List<AuthProvider> providers = new ArrayList<>();
        for (ClientRegistration registration : ClientRegistrations.list(registrationRepository.getIfAvailable())) {
            String id = registration.getRegistrationId();
            providers.add(new AuthProvider(id, label(id), "/oauth2/authorization/" + id));
        }
        providers.sort((a, b) -> Integer.compare(order(a.id()), order(b.id())));
        return providers;
    }

    private static int order(String id) {
        int index = PREFERRED_ORDER.indexOf(id);
        return index < 0 ? PREFERRED_ORDER.size() : index;
    }

    private static String label(String id) {
        return LABELS.getOrDefault(id, id.substring(0, 1).toUpperCase(Locale.ROOT) + id.substring(1));
    }

    /**
     * Resolves the local account for a validated provider profile.
     *
     * @param emailVerified whether the provider vouches that {@code email} is verified and owned
     *                      by the user; only then is an existing email account linked.
     */
    @Transactional
    public User resolveLogin(String provider, String subject, String email, boolean emailVerified,
                             String displayName, String avatarUrl) {
        Optional<User> existingIdentity =
                userRepository.findByOauthProviderAndOauthSubject(provider, subject);
        if (existingIdentity.isPresent()) {
            User user = existingIdentity.get();
            if (avatarUrl != null && !avatarUrl.isBlank()) {
                user.setAvatar(avatarUrl);
                userRepository.save(user);
            }
            return user;
        }
        if (email == null || email.isBlank()) {
            throw ApiException.badRequest(
                    "Your " + label(provider) + " account did not share an email address.");
        }
        Optional<User> byEmail = userRepository.findByEmailKey(PiiCrypto.emailKey(email));
        if (byEmail.isPresent()) {
            User user = byEmail.get();
            if (user.getOauthProvider() != null
                    && !user.getOauthProvider().equals(provider)) {
                throw ApiException.conflict(
                        "This email is already linked to a different sign-in provider.");
            }
            if (!emailVerified) {
                throw ApiException.conflict(
                        "An account already exists for this email. Log in with your password instead.");
            }
            user.setOauthProvider(provider);
            user.setOauthSubject(subject);
            if (avatarUrl != null && !avatarUrl.isBlank()) {
                user.setAvatar(avatarUrl);
            }
            if (!user.isEmailVerified()) {
                user.setEmailVerified(true);
            }
            userRepository.save(user);
            return user;
        }
        String username = uniqueUsername(email, provider);
        User user = new User(username,
                displayName != null && !displayName.isBlank() ? displayName.trim() : username,
                email, passwordEncoder.encode(UUID.randomUUID().toString()), provider, subject);
        user.setAuthRole(AuthRole.USER);
        user.setEmailVerified(true);
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            user.setAvatar(avatarUrl);
        }
        userRepository.save(user);
        return user;
    }

    private String uniqueUsername(String email, String provider) {
        String source = email.contains("@") ? email.substring(0, email.indexOf('@')) : provider;
        String base = source.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
        if (base.length() > 20) {
            base = base.substring(0, 20);
        }
        if (base.length() < 3) {
            base = "player";
        }
        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsernameIgnoreCase(candidate)) {
            String tail = "-" + suffix++;
            candidate = base.substring(0, Math.min(base.length(), 30 - tail.length())) + tail;
        }
        return candidate;
    }
}
