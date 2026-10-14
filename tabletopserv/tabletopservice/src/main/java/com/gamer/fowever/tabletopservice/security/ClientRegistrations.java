package com.gamer.fowever.tabletopservice.security;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link ClientRegistrationRepository} does not advertise its registrations in this Spring Security
 * version, so enumerate them by iterating the concrete repository when it supports it, and by
 * probing the ids we ship defaults for otherwise.
 */
public final class ClientRegistrations {

    private static final List<String> KNOWN_IDS = List.of("google", "github", "facebook");

    private ClientRegistrations() {
    }

    public static List<ClientRegistration> list(ClientRegistrationRepository repository) {
        if (repository == null) {
            return List.of();
        }
        List<ClientRegistration> registrations = new ArrayList<>();
        if (repository instanceof Iterable<?> iterable) {
            for (Object candidate : iterable) {
                if (candidate instanceof ClientRegistration registration) {
                    registrations.add(registration);
                }
            }
            return registrations;
        }
        for (String id : KNOWN_IDS) {
            ClientRegistration registration = repository.findByRegistrationId(id);
            if (registration != null) {
                registrations.add(registration);
            }
        }
        return registrations;
    }
}
