package com.gamer.fowever.tabletopfunctionaltest;

import com.gamer.fowever.tabletopfunctionaltest.support.Api;
import com.gamer.fowever.tabletopfunctionaltest.support.BackendExtension;
import com.gamer.fowever.tabletopfunctionaltest.support.BackendInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Common helpers for functional journeys: register→verify→login against the packaged
 * backend subprocess, then drive sessions/maps/etc. over its real HTTP interface.
 * Extending this base boots the backend once per fork JVM via {@link BackendExtension}.
 */
@ExtendWith(BackendExtension.class)
public abstract class FunctionalTestBase {

    protected static final String PASSWORD = "Password1!";
    protected static final String GAME_SLUG = "dnd-5e";

    protected static BackendInstance backend() {
        return BackendExtension.backend();
    }

    protected static String baseUrl() {
        return backend().baseUrl();
    }

    /**
     * Registers a fresh user, waits for the dev console verification email, verifies,
     * logs in, and returns a bearer JWT.
     */
    protected static String registerVerifyLogin(String username) throws Exception {
        String email = username + "@example.com";
        Api.requireStatus(Api.post(baseUrl() + "/api/auth/register", null,
                Api.body(Map.of("displayName", username, "email", email,
                        "dateOfBirth", "1990-01-15", "username", username, "password", PASSWORD))),
                201, "register " + username);

        String verifyUrl = backend().awaitVerificationUrl(email);
        assertThat(verifyUrl).as("verification email for %s", username).isNotBlank();
        Api.requireStatus(Api.get(verifyUrl, null), 200, "verify " + username);

        Api.Response login = Api.post(baseUrl() + "/api/auth/login", null,
                Api.body(Map.of("identifier", username, "password", PASSWORD)));
        Api.requireStatus(login, 200, "login " + username);
        return Api.json(login.body()).get("token").asText();
    }

    protected static SessionHandle createSession(String jwt, String name) {
        Api.Response created = Api.post(baseUrl() + "/api/sessions", jwt,
                Api.body(Map.of("name", name, "gameSlug", GAME_SLUG)));
        Api.requireStatus(created, 201, "create session " + name);
        var node = Api.json(created.body());
        return new SessionHandle(node.get("id").asLong(), node.get("inviteCode").asText());
    }

    protected static void joinSession(String jwt, String inviteCode) {
        Api.requireStatus(Api.post(baseUrl() + "/api/sessions/join", jwt,
                Api.body(Map.of("inviteCode", inviteCode))), 200, "join session");
    }

    public record SessionHandle(long id, String inviteCode) {
    }
}