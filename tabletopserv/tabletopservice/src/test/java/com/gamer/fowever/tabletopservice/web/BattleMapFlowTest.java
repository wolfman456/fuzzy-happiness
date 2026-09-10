package com.gamer.fowever.tabletopservice.web;

import com.gamer.fowever.tabletopservice.repository.EmailVerificationTokenRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BattleMapFlowTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Test
    void fullBattleMapFlow() throws Exception {
        String gmJwt = registerAndLogin("ginger");
        String playerJwt = registerAndLogin("ivo");
        SessionHandle session = createSession(gmJwt, "Grumm's Revenge");
        joinSession(playerJwt, session.inviteCode());

        mvc.perform(get("/api/sessions/" + session.id() + "/map").header("Authorization", "Bearer " + gmJwt))
                .andExpect(status().isNotFound());

        String created = mvc.perform(post("/api/sessions/" + session.id() + "/map")
                        .header("Authorization", "Bearer " + gmJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dungeon of Grumm\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Dungeon of Grumm"))
                .andExpect(jsonPath("$.squareFeet").value(10))
                .andExpect(jsonPath("$.tokens.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        assertThat((List<String>) JsonPath.read(created, "$.tokens[*].category"))
                .containsOnly("PLAYER");
        assertThat((List<Integer>) JsonPath.read(created, "$.tokens[*].speedFeet"))
                .containsOnly(30);

        mvc.perform(get("/api/sessions/" + session.id() + "/map").header("Authorization", "Bearer " + playerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokens.length()").value(2));

        mvc.perform(post("/api/sessions/" + session.id() + "/map")
                        .header("Authorization", "Bearer " + gmJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dungeon of Grumm\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokens.length()").value(2));

        mvc.perform(post("/api/sessions/" + session.id() + "/map/tokens")
                        .header("Authorization", "Bearer " + gmJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Goblin\",\"category\":\"MONSTER_NPC\",\"speedFeet\":30,\"x\":2,\"y\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokens.length()").value(3));

        String snapshot = mvc.perform(get("/api/sessions/" + session.id())
                        .header("Authorization", "Bearer " + gmJwt))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> recentTypes = JsonPath.read(snapshot, "$.recentEvents[*].type");
        assertThat(recentTypes).contains("PRESENCE");
        assertThat(recentTypes.stream().filter(type -> type.equals("TABLE")).count()).isEqualTo(2);

        long gingerTokenId = tokenIdByName(gmJwt, session.id(), "ginger");
        String moved = mvc.perform(post("/api/sessions/" + session.id() + "/map/tokens/" + gingerTokenId + "/move")
                        .header("Authorization", "Bearer " + gmJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":3,\"y\":1}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((List<Integer>) JsonPath.read(moved, "$.tokens[?(@.name == 'ginger')].movedFeet"))
                .containsExactly(20);

        long ivoTokenId = tokenIdByName(gmJwt, session.id(), "ivo");
        String ivoMoved = mvc.perform(post("/api/sessions/" + session.id() + "/map/tokens/" + ivoTokenId + "/move")
                        .header("Authorization", "Bearer " + playerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":5,\"y\":3}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((List<Integer>) JsonPath.read(ivoMoved, "$.tokens[?(@.name == 'ivo')].movedFeet"))
                .containsExactly(20);

        mvc.perform(post("/api/sessions/" + session.id() + "/map/tokens/" + ivoTokenId + "/move")
                        .header("Authorization", "Bearer " + playerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":7,\"y\":3}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/sessions/" + session.id() + "/map/tokens/" + gingerTokenId + "/move")
                        .header("Authorization", "Bearer " + playerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":5,\"y\":1}"))
                .andExpect(status().isForbidden());

        long goblinId = tokenIdByName(gmJwt, session.id(), "Goblin");
        mvc.perform(post("/api/sessions/" + session.id() + "/map/tokens/" + goblinId + "/move")
                        .header("Authorization", "Bearer " + playerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":6,\"y\":6}"))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/sessions/" + session.id() + "/map/tokens/" + goblinId)
                        .header("Authorization", "Bearer " + gmJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Goblin Captain\",\"speedFeet\":60}"))
                .andExpect(status().isOk());

        String afterTurnStart = mvc.perform(post("/api/sessions/" + session.id() + "/map/turn")
                        .header("Authorization", "Bearer " + gmJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"START\",\"tokenId\":" + gingerTokenId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentTurnTokenId").value(gingerTokenId))
                .andReturn().getResponse().getContentAsString();
        assertThat((List<Integer>) JsonPath.read(afterTurnStart, "$.tokens[?(@.name == 'ginger')].movedFeet"))
                .containsExactly(0);

        mvc.perform(post("/api/sessions/" + session.id() + "/map/turn")
                        .header("Authorization", "Bearer " + gmJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"END\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentTurnTokenId").isEmpty());

        String afterNewRound = mvc.perform(post("/api/sessions/" + session.id() + "/map/turn")
                        .header("Authorization", "Bearer " + gmJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"NEW_ROUND\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((List<Integer>) JsonPath.read(afterNewRound, "$.tokens[*].movedFeet"))
                .allMatch(movedFeet -> movedFeet == 0);

        String patched = mapBody(gmJwt, session.id());
        assertThat((List<Integer>) JsonPath.read(patched, "$.tokens[?(@.name == 'Goblin Captain')].speedFeet"))
                .containsExactly(60);

        mvc.perform(patch("/api/sessions/" + session.id() + "/map")
                        .header("Authorization", "Bearer " + gmJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"width\":12,\"height\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.width").value(12))
                .andExpect(jsonPath("$.height").value(10));

        mvc.perform(patch("/api/sessions/" + session.id() + "/map")
                        .header("Authorization", "Bearer " + playerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"width\":12,\"height\":10}"))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/sessions/" + session.id() + "/map")
                        .header("Authorization", "Bearer " + gmJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"width\":1,\"height\":1}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mvc.perform(delete("/api/sessions/" + session.id() + "/map/tokens/" + goblinId)
                        .header("Authorization", "Bearer " + gmJwt))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/sessions/" + session.id() + "/map")
                        .header("Authorization", "Bearer " + gmJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokens.length()").value(2));
    }

    @Test
    void initiativeFlowForGmOnly() throws Exception {
        String gmJwt = registerAndLogin("ginger");
        String playerJwt = registerAndLogin("ivo");
        SessionHandle session = createSession(gmJwt, "Order Room");
        joinSession(playerJwt, session.inviteCode());

        mvc.perform(post("/api/sessions/" + session.id() + "/map")
                        .header("Authorization", "Bearer " + gmJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Initiative\"}"))
                .andExpect(status().isCreated());

        String ivoId = String.valueOf(tokenIdByName(gmJwt, session.id(), "ivo"));
        String gingerId = String.valueOf(tokenIdByName(gmJwt, session.id(), "ginger"));

        mvc.perform(post("/api/sessions/" + session.id() + "/map/initiative")
                        .header("Authorization", "Bearer " + playerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entries\":[]}"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/sessions/" + session.id() + "/map/initiative")
                        .header("Authorization", "Bearer " + gmJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entries\":["
                                + "{\"tokenId\":" + gingerId + ",\"score\":18},"
                                + "{\"label\":\"Orc\",\"score\":14},"
                                + "{\"tokenId\":" + ivoId + ",\"score\":9}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initiative.length()").value(3))
                .andExpect(jsonPath("$.initiative[0].tokenName").value("ginger"))
                .andExpect(jsonPath("$.initiative[0].score").value(18))
                .andExpect(jsonPath("$.initiative[?(@.label == 'Orc')].score").value(org.hamcrest.Matchers.contains(14)))
                .andExpect(jsonPath("$.initiative[?(@.tokenName == 'ivo')].score").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(Integer.valueOf(1)))))
                .andExpect(jsonPath("$.initiative[?(@.tokenName == 'ivo')].score").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.lessThanOrEqualTo(Integer.valueOf(20)))));

        String nextBody = mvc.perform(post("/api/sessions/" + session.id() + "/map/initiative/next")
                        .header("Authorization", "Bearer " + gmJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initiativeIndex").value(0))
                .andExpect(jsonPath("$.currentTurnTokenId").value(gingerId))
                .andReturn().getResponse().getContentAsString();
        String ivoEntryId = String.valueOf(JsonPath
                .<List<?>>read(nextBody, "$.initiative[?(@.tokenName == 'ivo')].id").getFirst());

        mvc.perform(post("/api/sessions/" + session.id() + "/map/initiative/" + ivoEntryId + "/reroll")
                        .header("Authorization", "Bearer " + gmJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initiative[?(@.id == " + ivoEntryId + ")].score")
                        .value(org.hamcrest.Matchers.everyItem(
                                org.hamcrest.Matchers.greaterThanOrEqualTo(Integer.valueOf(1))))
                        )
                .andExpect(jsonPath("$.initiative[?(@.id == " + ivoEntryId + ")].score")
                        .value(org.hamcrest.Matchers.everyItem(
                                org.hamcrest.Matchers.lessThanOrEqualTo(Integer.valueOf(20))))
                        );

        mvc.perform(delete("/api/sessions/" + session.id() + "/map/initiative/" + ivoEntryId)
                        .header("Authorization", "Bearer " + gmJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initiative.length()").value(2));

        mvc.perform(post("/api/sessions/" + session.id() + "/map/initiative/next")
                        .header("Authorization", "Bearer " + playerJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void mapManagementEndpointsRequireGm() throws Exception {
        String gmJwt = registerAndLogin("ginger");
        String playerJwt = registerAndLogin("ivo");
        SessionHandle session = createSession(gmJwt, "Room");
        joinSession(playerJwt, session.inviteCode());

        mvc.perform(post("/api/sessions/" + session.id() + "/map")
                        .header("Authorization", "Bearer " + playerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nope\"}"))
                .andExpect(status().isForbidden());

        String gmCreate = mvc.perform(post("/api/sessions/" + session.id() + "/map")
                        .header("Authorization", "Bearer " + gmJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dungeon\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long playerTokenId = ((Number) JsonPath.read(gmCreate, "$.tokens[0].id")).longValue();

        mvc.perform(post("/api/sessions/" + session.id() + "/map/tokens")
                        .header("Authorization", "Bearer " + playerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Goblin\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/sessions/" + session.id() + "/map/tokens/" + playerTokenId)
                        .header("Authorization", "Bearer " + playerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hijacked\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(delete("/api/sessions/" + session.id() + "/map/tokens/" + playerTokenId)
                        .header("Authorization", "Bearer " + playerJwt))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/sessions/" + session.id() + "/map/turn")
                        .header("Authorization", "Bearer " + playerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"END\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void mapEndpointsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/sessions/1/map"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/sessions/1/map")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dungeon\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createMapValidatesInvalidSize() throws Exception {
        String gmJwt = registerAndLogin("ginger");
        SessionHandle session = createSession(gmJwt, "Room");

        mvc.perform(post("/api/sessions/" + session.id() + "/map")
                        .header("Authorization", "Bearer " + gmJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tiny\",\"width\":300,\"height\":18}"))
                .andExpect(status().isBadRequest());
    }

    private String mapBody(String jwt, long sessionId) throws Exception {
        return mvc.perform(get("/api/sessions/" + sessionId + "/map")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private long tokenIdByName(String jwt, long sessionId, String name) throws Exception {
        List<Integer> ids = JsonPath.read(mapBody(jwt, sessionId),
                "$.tokens[?(@.name == '" + name + "')].id");
        return ids.getFirst();
    }

    private SessionHandle createSession(String jwt, String name) throws Exception {
        MvcResult create = mvc.perform(post("/api/sessions")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"gameSlug\":\"dnd-5e\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String body = create.getResponse().getContentAsString();
        return new SessionHandle(
                ((Number) JsonPath.read(body, "$.id")).longValue(),
                JsonPath.read(body, "$.inviteCode"));
    }

    private void joinSession(String jwt, String inviteCode) throws Exception {
        mvc.perform(post("/api/sessions/join")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inviteCode\":\"" + inviteCode + "\"}"))
                .andExpect(status().isOk());
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

    private record SessionHandle(long id, String inviteCode) {
    }
}