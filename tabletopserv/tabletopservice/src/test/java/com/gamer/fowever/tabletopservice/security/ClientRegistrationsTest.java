package com.gamer.fowever.tabletopservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClientRegistrationsTest {

    private static ClientRegistration registration(String id) {
        return ClientRegistration.withRegistrationId(id)
                .clientId("id").clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("email").authorizationUri("https://example.com/auth")
                .tokenUri("https://example.com/token")
                .userNameAttributeName("sub").build();
    }

    @Test
    void emptyWhenRepositoryMissing() {
        assertThat(ClientRegistrations.list(null)).isEmpty();
    }

    @Test
    void iteratesConcreteRepository() {
        ClientRegistrationRepository repository = new InMemoryClientRegistrationRepository(
                registration("google"), registration("github"));

        assertThat(ClientRegistrations.list(repository))
                .extracting(ClientRegistration::getRegistrationId)
                .containsExactlyInAnyOrder("google", "github");
    }

    @Test
    void ignoresNonRegistrationElementsInIterable() {
        ClientRegistrationRepository repository = new MixedIterableRepository();

        assertThat(ClientRegistrations.list(repository))
                .extracting(ClientRegistration::getRegistrationId)
                .containsExactly("google");
    }

    @Test
    void probesKnownIdsWhenNotIterable() {
        ClientRegistrationRepository repository = id -> "github".equals(id) ? registration("github") : null;

        assertThat(ClientRegistrations.list(repository))
                .extracting(ClientRegistration::getRegistrationId)
                .containsExactly("github");
    }

    @Test
    void emptyWhenNonIterableHasNoKnownIds() {
        ClientRegistrationRepository repository = id -> null;

        assertThat(ClientRegistrations.list(repository)).isEmpty();
    }

    private static final class MixedIterableRepository implements ClientRegistrationRepository, Iterable<Object> {
        @Override
        public ClientRegistration findByRegistrationId(String registrationId) {
            return null;
        }

        @Override
        public Iterator<Object> iterator() {
            return List.<Object>of(registration("google"), "not-a-registration").iterator();
        }
    }
}
