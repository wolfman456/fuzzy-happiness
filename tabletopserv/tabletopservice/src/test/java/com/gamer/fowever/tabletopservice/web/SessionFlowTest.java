package com.gamer.fowever.tabletopservice.web;

import com.gamer.fowever.tabletopservice.domain.GameSession;
import com.gamer.fowever.tabletopapi.SessionStatus;
import com.gamer.fowever.tabletopservice.repository.EmailVerificationTokenRepository;
import com.gamer.fowever.tabletopservice.repository.GameSessionRepository;
import com.gamer.fowever.tabletopservice.repository.UserRepository;
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
class SessionFlowTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmailVerificationTokenRepository tokenRepository;
    @Autowired
    private GameSessionRepository sessionRepository;

    @Test
    void createJoinSnapshotAndLeaveFlow() throws Exception {
        String gmJwt = registerAndLogin("ginger");
        String playerJwt = registerAndLogin("ivo");

        MvcResult create = mvc.perform(post("/api/sessions")
                        .header("Authorization", "Bearer " + gmJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Grumm's Revenge\",\"gameSlug\":\"dnd-5e\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.gameSlug").value("dnd-5e"))
                .andExpect(jsonPath("$.inviteCode").isNotEmpty())
                .andExpect(jsonPath("$.participants[0].role").value("GM"))
                .andReturn();
        String inviteCode = JsonPath.read(create.getResponse().getContentAsString(), "$.inviteCode");
        long sessionId = ((Number) JsonPath.read(create.getResponse().getContentAsString(), "$.id")).longValue();

        mvc.perform(post("/api/sessions/join")
                        .header("Authorization", "Bearer " + playerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inviteCode\":\"" + inviteCode + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants.length()").value(2))
                .andExpect(jsonPath("$.recentEvents[0].type").value("PRESENCE"));

        mvc.perform(post("/api/sessions/join")
                        .header("Authorization", "Bearer " + playerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inviteCode\":\"" + inviteCode + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants.length()").value(2));

        mvc.perform(get("/api/sessions/" + sessionId)
                        .header("Authorization", "Bearer " + playerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants.length()").value(2))
                .andExpect(jsonPath("$.createdBy.username").value("ginger"));

        mvc.perform(post("/api/sessions/" + sessionId + "/leave")
                        .header("Authorization", "Bearer " + playerJwt))
                .andExpect(status().isAccepted());

        mvc.perform(get("/api/sessions/" + sessionId)
                        .header("Authorization", "Bearer " + gmJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants.length()").value(1));
    }

    @Test
    void createSessionRejectsUnknownGame() throws Exception {
        String jwt = registerAndLogin("ginger");

        mvc.perform(post("/api/sessions")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Room\",\"gameSlug\":\"nope\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createSessionRequiresValidation() throws Exception {
        String jwt = registerAndLogin("ginger");

        mvc.perform(post("/api/sessions")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"gameSlug\":\"dnd-5e\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void joinSessionRejectsUnknownInviteCode() throws Exception {
        String jwt = registerAndLogin("ginger");

        mvc.perform(post("/api/sessions/join")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inviteCode\":\"NOPE12\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void snapshotRejectsNonMember() throws Exception {
        String ownerJwt = registerAndLogin("ginger");
        String outsiderJwt = registerAndLogin("stranger");

        MvcResult create = mvc.perform(post("/api/sessions")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Room\",\"gameSlug\":\"dnd-5e\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long sessionId = ((Number) JsonPath.read(create.getResponse().getContentAsString(), "$.id")).longValue();

        mvc.perform(get("/api/sessions/" + sessionId)
                        .header("Authorization", "Bearer " + outsiderJwt))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/sessions/" + sessionId + "/leave")
                        .header("Authorization", "Bearer " + outsiderJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void sessionEndpointsRequireAuthentication() throws Exception {
        mvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Room\",\"gameSlug\":\"dnd-5e\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/sessions/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void lastParticipantLeavingClosesSession() throws Exception {
        String jwt = registerAndLogin("ginger");
        MvcResult create = mvc.perform(post("/api/sessions")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Room\",\"gameSlug\":\"dnd-5e\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long sessionId = ((Number) JsonPath.read(create.getResponse().getContentAsString(), "$.id")).longValue();

        mvc.perform(post("/api/sessions/" + sessionId + "/leave")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isAccepted());

        GameSession reloaded = sessionRepository.findById(sessionId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(SessionStatus.CLOSED);
    }

    @Test
    void gamesListsSeededDnd5e() throws Exception {
        String jwt = registerAndLogin("ginger");

        mvc.perform(get("/api/games").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.slug == 'dnd-5e')]").exists());
    }

    private String registerAndLogin(String username) throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"" + username + "\","
                                + "\"realName\":\"Real " + username + "\","
                                + "\"email\":\"" + username + "@example.com\","
                                + "\"dateOfBirth\":\"1990-01-15\","
                                + "\"username\":\"" + username + "\","
                                + "\"password\":\"Password1!\","
                                + "\"confirmPassword\":\"Password1!\"}"))
                .andExpect(status().isCreated());

        List<com.gamer.fowever.tabletopservice.domain.EmailVerificationToken> tokens = tokenRepository.findAll();
        String token = tokens.getLast().getToken();
        mvc.perform(get("/api/auth/verify").param("token", token))
                .andExpect(status().isOk());

        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"" + username + "\",\"password\":\"Password1!\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(login.getResponse().getContentAsString(), "$.token");
    }
}