package com.gamer.fowever.tabletopfunctionaltest.auth;

import com.gamer.fowever.tabletopfunctionaltest.FunctionalTestBase;
import com.gamer.fowever.tabletopfunctionaltest.support.Api;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthJourneyIT extends FunctionalTestBase {

    private static final String WEAK = "short";

    @Test
    void fullRegisterVerifyLoginFlow() throws Exception {
        String username = "itagm";
        String email = username + "@example.com";
        Api.requireStatus(Api.post(baseUrl() + "/api/auth/register", null,
                Api.body(Map.of("displayName", "Aria", "realName", "Aria Ashton", "email", email,
                        "dateOfBirth", "1990-01-15", "username", username,
                        "password", PASSWORD, "confirmPassword", PASSWORD))),
                201, "register");

        Api.Response preVerify = Api.post(baseUrl() + "/api/auth/login", null,
                Api.body(Map.of("identifier", username, "password", PASSWORD)));
        assertThat(preVerify.status())
                .as("login must be rejected until the email is verified").isEqualTo(403);

        String verifyUrl = backend().awaitVerificationUrl(email);
        assertThat(verifyUrl).as("dev console email must carry the verify link").isNotBlank();
        Api.requireStatus(Api.get(verifyUrl, null), 200, "verify");

        Api.Response login = Api.post(baseUrl() + "/api/auth/login", null,
                Api.body(Map.of("identifier", email, "password", PASSWORD)));
        Api.requireStatus(login, 200, "login after verification");
        String jwt = Api.json(login.body()).get("token").asText();

        JsonNode me = Api.json(Api.get(baseUrl() + "/api/users/me", jwt).body());
        assertThat(me.get("username").asText()).isEqualTo(username);
        assertThat(me.get("emailVerified").asBoolean()).isTrue();

        Api.Response admin = Api.get(baseUrl() + "/api/admin/users", jwt);
        assertThat(admin.status()).as("non-admin must be denied the admin API").isEqualTo(403);
    }

    @Test
    void rejectsWeakPasswordAndUnderage() {
        Api.Response weak = Api.post(baseUrl() + "/api/auth/register", null,
                Api.body(Map.of("displayName", "Weak", "realName", "Weak W", "email", "weak@example.com",
                        "dateOfBirth", "1990-01-15", "username", "w1", "password", WEAK,
                        "confirmPassword", WEAK)));
        assertThat(weak.status()).isEqualTo(400);

        Api.Response underage = Api.post(baseUrl() + "/api/auth/register", null,
                Api.body(Map.of("displayName", "Kid", "realName", "Kid K", "email", "kid@example.com",
                        "dateOfBirth", "2015-01-01", "username", "k1",
                        "password", PASSWORD, "confirmPassword", PASSWORD)));
        assertThat(underage.status()).isEqualTo(400);
    }

    @Test
    void duplicateUsernameIsConflict() {
        Api.requireStatus(Api.post(baseUrl() + "/api/auth/register", null,
                Api.body(Map.of("displayName", "Dup", "realName", "Dup D", "email", "dup@example.com",
                        "dateOfBirth", "1990-01-15", "username", "itadup",
                        "password", PASSWORD, "confirmPassword", PASSWORD))),
                201, "first register");

        Api.Response duplicate = Api.post(baseUrl() + "/api/auth/register", null,
                Api.body(Map.of("displayName", "Dup", "realName", "Dup D", "email", "other@example.com",
                        "dateOfBirth", "1990-01-15", "username", "itadup",
                        "password", PASSWORD, "confirmPassword", PASSWORD)));
        assertThat(duplicate.status()).isEqualTo(409);
    }

    @Test
    void loginRejectsBadCredentials() {
        Api.Response bad = Api.post(baseUrl() + "/api/auth/login", null,
                Api.body(Map.of("identifier", "missing-user", "password", PASSWORD)));
        assertThat(bad.status()).isEqualTo(401);
    }

    @Test
    void verifyRejectsUnknownToken() {
        assertThat(Api.get(baseUrl() + "/api/auth/verify?token=does-not-exist", null).status()).isEqualTo(400);
        assertThat(Api.get(baseUrl() + "/api/auth/verify", null).status()).isEqualTo(400);
    }

    @Test
    void adminCanListUsers() {
        Api.Response login = Api.post(baseUrl() + "/api/auth/login", null,
                Api.body(Map.of("identifier", "admin", "password", "AdminPassw0rd!")));
        Api.requireStatus(login, 200, "bootstrap admin login");
        String jwt = Api.json(login.body()).get("token").asText();

        Api.Response users = Api.get(baseUrl() + "/api/admin/users", jwt);
        Api.requireStatus(users, 200, "admin users list");
        assertThat(Api.json(users.body()).isArray()).isTrue();
    }

    @Test
    void protectedEndpointsRejectMissingAndInvalidTokens() {
        assertThat(Api.get(baseUrl() + "/api/users/me", null).status()).isEqualTo(401);
        assertThat(Api.get(baseUrl() + "/api/users/me", "not.a.jwt").status()).isEqualTo(401);
    }

    @Test
    void userCanChangeOwnUsername() throws Exception {
        String username = "itarename";
        String email = username + "@example.com";
        Api.requireStatus(Api.post(baseUrl() + "/api/auth/register", null,
                Api.body(Map.of("displayName", "Rena", "realName", "Rena R", "email", email,
                        "dateOfBirth", "1990-01-15", "username", username,
                        "password", PASSWORD, "confirmPassword", PASSWORD))),
                201, "register");

        String verifyUrl = backend().awaitVerificationUrl(email);
        Api.requireStatus(Api.get(verifyUrl, null), 200, "verify");

        Api.Response login = Api.post(baseUrl() + "/api/auth/login", null,
                Api.body(Map.of("identifier", username, "password", PASSWORD)));
        String jwt = Api.json(login.body()).get("token").asText();

        String newUsername = "itarename2";
        Api.Response renamed = Api.patch(baseUrl() + "/api/users/me/username", jwt,
                Api.body(Map.of("username", newUsername)));
        Api.requireStatus(renamed, 200, "rename");
        assertThat(Api.json(renamed.body()).get("username").asText()).isEqualTo(newUsername);

        JsonNode me = Api.json(Api.get(baseUrl() + "/api/users/me", jwt).body());
        assertThat(me.get("username").asText()).isEqualTo(newUsername);

        Api.Response relogin = Api.post(baseUrl() + "/api/auth/login", null,
                Api.body(Map.of("identifier", newUsername, "password", PASSWORD)));
        Api.requireStatus(relogin, 200, "login with renamed username");

        Api.Response second = Api.patch(baseUrl() + "/api/users/me/username", jwt,
                Api.body(Map.of("username", newUsername)));
        Api.requireStatus(second, 200, "noop rename");
        assertThat(Api.json(second.body()).get("username").asText()).isEqualTo(newUsername);
    }

    @Test
    void usernameChangeRejectsTakenName() throws Exception {
        Api.requireStatus(Api.post(baseUrl() + "/api/auth/register", null,
                Api.body(Map.of("displayName", "A", "realName", "A A", "email", "itaa@example.com",
                        "dateOfBirth", "1990-01-15", "username", "reat1",
                        "password", PASSWORD, "confirmPassword", PASSWORD))),
                201, "register one");
        Api.requireStatus(Api.post(baseUrl() + "/api/auth/register", null,
                Api.body(Map.of("displayName", "B", "realName", "B B", "email", "itbb@example.com",
                        "dateOfBirth", "1990-01-15", "username", "reat2",
                        "password", PASSWORD, "confirmPassword", PASSWORD))),
                201, "register two");
        String verifyUrl = backend().awaitVerificationUrl("itaa@example.com");
        Api.requireStatus(Api.get(verifyUrl, null), 200, "verify one");

        Api.Response login = Api.post(baseUrl() + "/api/auth/login", null,
                Api.body(Map.of("identifier", "reat1", "password", PASSWORD)));
        String jwt = Api.json(login.body()).get("token").asText();

        Api.Response conflict = Api.patch(baseUrl() + "/api/users/me/username", jwt,
                Api.body(Map.of("username", "reat2")));
        assertThat(conflict.status()).isEqualTo(409);
    }

    @Test
    void editsProfileAndChangesPassword() throws Exception {
        String username = "itaprofile";
        String email = username + "@example.com";
        Api.requireStatus(Api.post(baseUrl() + "/api/auth/register", null,
                Api.body(Map.of("displayName", "Prof", "realName", "Prof P", "email", email,
                        "dateOfBirth", "1990-01-15", "username", username,
                        "password", PASSWORD, "confirmPassword", PASSWORD))),
                201, "register");

        String verifyUrl = backend().awaitVerificationUrl(email);
        Api.requireStatus(Api.get(verifyUrl, null), 200, "verify");

        Api.Response login = Api.post(baseUrl() + "/api/auth/login", null,
                Api.body(Map.of("identifier", username, "password", PASSWORD)));
        String jwt = Api.json(login.body()).get("token").asText();

        Api.Response profile = Api.patch(baseUrl() + "/api/users/me/profile", jwt,
                Api.body(Map.of("displayName", "The Bard", "realName", "Prof Pickles")));
        Api.requireStatus(profile, 200, "update profile");
        assertThat(Api.json(profile.body()).get("displayName").asText()).isEqualTo("The Bard");

        JsonNode me = Api.json(Api.get(baseUrl() + "/api/users/me", jwt).body());
        assertThat(me.get("displayName").asText()).isEqualTo("The Bard");
        assertThat(me.get("realName").asText()).isEqualTo("Prof Pickles");

        String newPassword = "BrandNew3x!";
        Api.requireStatus(Api.patch(baseUrl() + "/api/users/me/password", jwt,
                Api.body(Map.of("currentPassword", PASSWORD, "newPassword", newPassword,
                        "confirmPassword", newPassword))),
                200, "change password");

        Api.Response relogin = Api.post(baseUrl() + "/api/auth/login", null,
                Api.body(Map.of("identifier", username, "password", newPassword)));
        Api.requireStatus(relogin, 200, "login with new password");
    }
}