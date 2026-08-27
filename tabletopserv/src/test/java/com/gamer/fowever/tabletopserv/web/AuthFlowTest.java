package com.gamer.fowever.tabletopserv.web;

import com.gamer.fowever.tabletopserv.domain.EmailVerificationToken;
import com.gamer.fowever.tabletopserv.repository.EmailVerificationTokenRepository;
import com.gamer.fowever.tabletopserv.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthFlowTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private EmailVerificationTokenRepository tokenRepository;
    @Autowired
    private UserRepository userRepository;

    private static final String REGISTER_BODY = """
            {"displayName":"Aria","email":"aria@example.com","dateOfBirth":"1990-01-15",\
            "username":"aria","password":"Password1!"}
            """;

    private static final String ADMIN_BODY = """
            {"identifier":"admin","password":"AdminPassw0rd!"}
            """;

    @Test
    void fullRegisterVerifyLoginFlow() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("verify")));

        List<EmailVerificationToken> tokens = tokenRepository.findAll();
        assertThat(tokens).hasSize(1);
        assertThat(userRepository.findByUsernameIgnoreCase("aria").orElseThrow().isEmailVerified()).isFalse();
        String token = tokens.get(0).getToken();

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"aria@example.com\",\"password\":\"Password1!\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/auth/verify").param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Email verified. You can now log in."));
        assertThat(tokenRepository.count()).isZero();
        assertThat(userRepository.findByUsernameIgnoreCase("aria").orElseThrow().isEmailVerified()).isTrue();

        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"aria\",\"password\":\"Password1!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.username").value("aria"))
                .andReturn();
        String jwt = JsonPath.read(login.getResponse().getContentAsString(), "$.token");

        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("aria"))
                .andExpect(jsonPath("$.emailVerified").value(true));

        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsWeakPasswordAndUnderage() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Aria\",\"email\":\"a@example.com\","
                                + "\"dateOfBirth\":\"1990-01-15\",\"username\":\"weak\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Kid\",\"email\":\"kid@example.com\","
                                + "\"dateOfBirth\":\"2015-01-01\",\"username\":\"kid\",\"password\":\"Password1!\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsDuplicateUsernameWithConflict() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void loginRejectsBadCredentials() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"missing\",\"password\":\"Password1!\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void resendVerificationIsEnumerationSafeAndCooldownEnforced() throws Exception {
        mvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"missing-user\"}"))
                .andExpect(status().isAccepted());

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"aria\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void verifyRejectsUnknownToken() throws Exception {
        mvc.perform(get("/api/auth/verify").param("token", "does-not-exist"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/auth/verify"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCanListUsers() throws Exception {
        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(ADMIN_BODY))
                .andExpect(status().isOk())
                .andReturn();
        String jwt = JsonPath.read(login.getResponse().getContentAsString(), "$.token");

        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void protectedEndpointsRejectMissingAndInvalidTokens() throws Exception {
        mvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/users/me").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }
}