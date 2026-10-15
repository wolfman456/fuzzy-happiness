package com.gamer.fowever.tabletopfunctionaltest.battle;

import com.gamer.fowever.tabletopfunctionaltest.FunctionalTestBase;
import com.gamer.fowever.tabletopfunctionaltest.support.Api;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Battle-map journeys on the packaged backend: map creation seeds player tokens,
 * token movement respects the per-turn budget, turn/round management works, and
 * initiative ordering is GM-only.
 */
class BattleMapJourneyIT extends FunctionalTestBase {

    @Test
    void fullBattleMapFlow() throws Exception {
        String gmJwt = registerVerifyLogin("b1gm");
        String playerJwt = registerVerifyLogin("b1pip");
        SessionHandle session = createSession(gmJwt, "Grumm's Revenge");
        joinSession(playerJwt, session.inviteCode());

        Api.Response noMap = Api.get(baseUrl() + "/api/sessions/" + session.id() + "/map", gmJwt);
        assertThat(noMap.status()).isEqualTo(404);
        assertThat(Api.json(noMap.body()).at("/message").asText()).contains("No battle map");

        JsonNode created = Api.json(Api.post(baseUrl() + "/api/sessions/" + session.id() + "/map", gmJwt,
                Api.body(Map.of("name", "Dungeon of Grumm"))).body());
        assertThat(created.get("name").asText()).isEqualTo("Dungeon of Grumm");
        assertThat(created.get("tokens").size()).isEqualTo(2);

        JsonNode playerView = get(session.id(), playerJwt);
        assertThat(playerView.get("tokens").size()).isEqualTo(2);

        JsonNode withGoblin = Api.json(Api.post(baseUrl() + "/api/sessions/" + session.id() + "/map/tokens", gmJwt,
                Api.body(Map.of("name", "Goblin", "category", "MONSTER_NPC", "speedFeet", 30, "x", 2, "y", 2))).body());
        assertThat(withGoblin.get("tokens").size()).isEqualTo(3);

        long gingerTokenId = tokenIdByName(session.id(), gmJwt, "b1gm");
        long ivoTokenId = tokenIdByName(session.id(), gmJwt, "b1pip");

        JsonNode gingerMove = moveToken(session.id(), gmJwt, gingerTokenId, 3, 1);
        assertThat(gingerMove.at("/tokens/0/movedFeet").asInt()).isEqualTo(20);
        assertThat(gingerMove.at("/tokens/0/posX").asInt()).isEqualTo(3);
        assertThat(gingerMove.at("/tokens/0/posY").asInt()).isEqualTo(1);

        JsonNode ivoMove = moveToken(session.id(), playerJwt, ivoTokenId, 5, 3);
        assertThat(ivoMove.at("/tokens/1/movedFeet").asInt()).isEqualTo(20);

        Api.Response overBudget = Api.post(baseUrl() + "/api/sessions/" + session.id()
                + "/map/tokens/" + ivoTokenId + "/move", playerJwt, Api.body(Map.of("x", 7, "y", 3)));
        assertThat(overBudget.status()).isEqualTo(400);

        Api.Response playerMovesGmToken = Api.post(baseUrl() + "/api/sessions/" + session.id()
                + "/map/tokens/" + gingerTokenId + "/move", playerJwt, Api.body(Map.of("x", 5, "y", 1)));
        assertThat(playerMovesGmToken.status()).isEqualTo(403);

        long goblinId = tokenIdByName(session.id(), gmJwt, "Goblin");
        Api.Response playerMovesMonster = Api.post(baseUrl() + "/api/sessions/" + session.id()
                + "/map/tokens/" + goblinId + "/move", playerJwt, Api.body(Map.of("x", 6, "y", 6)));
        assertThat(playerMovesMonster.status()).isEqualTo(403);

        Api.requireStatus(Api.patch(baseUrl() + "/api/sessions/" + session.id() + "/map/tokens/" + goblinId,
                gmJwt, Api.body(Map.of("name", "Goblin Captain", "speedFeet", 60))), 200, "rename token");

        Api.Response firstTurn = Api.post(baseUrl() + "/api/sessions/" + session.id() + "/map/turn", gmJwt,
                Api.body(Map.of("action", "START", "tokenId", gingerTokenId)));
        Api.requireStatus(firstTurn, 200, "start turn");
        JsonNode afterStart = Api.json(firstTurn.body());
        assertThat(afterStart.get("currentTurnTokenId").asLong()).isEqualTo(gingerTokenId);
        assertThat(afterStart.at("/tokens/0/movedFeet").asInt()).isZero();

        JsonNode afterEnd = Api.json(Api.post(baseUrl() + "/api/sessions/" + session.id() + "/map/turn", gmJwt,
                Api.body(Map.of("action", "END"))).body());
        assertThat(afterEnd.get("currentTurnTokenId").isNull()).isTrue();

        JsonNode afterNewRound = Api.json(Api.post(baseUrl() + "/api/sessions/" + session.id() + "/map/turn", gmJwt,
                Api.body(Map.of("action", "NEW_ROUND"))).body());
        for (JsonNode token : afterNewRound.get("tokens")) {
            assertThat(token.get("movedFeet").asInt()).isZero();
        }

        JsonNode renamed = get(session.id(), gmJwt);
        JsonNode captain = first(renamed, "Goblin Captain");
        assertThat(captain.get("speedFeet").asInt()).isEqualTo(60);

        Api.requireStatus(Api.patch(baseUrl() + "/api/sessions/" + session.id() + "/map", gmJwt,
                Api.body(Map.of("width", 12, "height", 10))), 200, "resize map");
        JsonNode resized = get(session.id(), gmJwt);
        assertThat(resized.get("width").asInt()).isEqualTo(12);
        assertThat(resized.get("height").asInt()).isEqualTo(10);

        Api.requireStatus(Api.delete(baseUrl() + "/api/sessions/" + session.id() + "/map/tokens/" + goblinId, gmJwt),
                204, "remove token");
        assertThat(get(session.id(), gmJwt).get("tokens").size()).isEqualTo(2);
    }

    @Test
    void initiativeFlowIsGmOnly() throws Exception {
        String gmJwt = registerVerifyLogin("b2gm");
        String playerJwt = registerVerifyLogin("b2pip");
        SessionHandle session = createSession(gmJwt, "Order Room");
        joinSession(playerJwt, session.inviteCode());

        Api.requireStatus(Api.post(baseUrl() + "/api/sessions/" + session.id() + "/map", gmJwt,
                Api.body(Map.of("name", "Initiative"))), 201, "create map");

        long ivoId = tokenIdByName(session.id(), gmJwt, "b2pip");
        long gingerId = tokenIdByName(session.id(), gmJwt, "b2gm");

        Api.Response playerSets = Api.post(baseUrl() + "/api/sessions/" + session.id() + "/map/initiative",
                playerJwt, Api.body(Map.of("entries", java.util.List.of())));
        assertThat(playerSets.status()).isEqualTo(403);

        JsonNode entries = Api.json(Api.post(baseUrl() + "/api/sessions/" + session.id() + "/map/initiative", gmJwt,
                Api.body(Map.of("entries", java.util.List.of(
                        Map.of("tokenId", gingerId, "score", 18),
                        Map.of("label", "Orc", "score", 14),
                        Map.of("tokenId", ivoId, "score", 9))))).body());
        assertThat(entries.get("initiative").size()).isEqualTo(3);
        assertThat(entries.at("/initiative/0/tokenName").asText()).isEqualTo("b2gm");
        assertThat(entries.at("/initiative/0/score").asInt()).isEqualTo(18);
        assertThat(entries.at("/initiative/1/label").asText()).isEqualTo("Orc");
        assertThat(entries.at("/initiative/2/tokenName").asText()).isEqualTo("b2pip");

        JsonNode afterNext = Api.json(Api.post(baseUrl() + "/api/sessions/" + session.id()
                + "/map/initiative/next", gmJwt, null).body());
        assertThat(afterNext.get("initiativeIndex").asInt()).isZero();
        assertThat(afterNext.get("currentTurnTokenId").asLong()).isEqualTo(gingerId);

        long ivoEntryId = afterNext.at("/initiative/2/id").asLong();
        JsonNode rerolled = Api.json(Api.post(baseUrl() + "/api/sessions/" + session.id()
                + "/map/initiative/" + ivoEntryId + "/reroll", gmJwt, null).body());
        for (JsonNode entry : rerolled.get("initiative")) {
            if (entry.get("id").asLong() == ivoEntryId) {
                assertThat(entry.get("score").asInt()).isBetween(1, 20);
            }
        }

        JsonNode afterDelete = Api.json(Api.delete(baseUrl() + "/api/sessions/" + session.id()
                + "/map/initiative/" + ivoEntryId, gmJwt).body());
        assertThat(afterDelete.get("initiative").size()).isEqualTo(2);

        Api.requireStatus(Api.post(baseUrl() + "/api/sessions/" + session.id() + "/map/initiative/next",
                playerJwt, null), 403, "player initiative next");
    }

    @Test
    void setupPhaseLetsPlayersPlaceTheirOwnTokensUntilCombatStarts() throws Exception {
        String gmJwt = registerVerifyLogin("b3gm");
        String playerJwt = registerVerifyLogin("b3pip");
        SessionHandle session = createSession(gmJwt, "Open Field");
        joinSession(playerJwt, session.inviteCode());

        Api.requireStatus(Api.post(baseUrl() + "/api/sessions/" + session.id() + "/map", gmJwt,
                Api.body(Map.of("name", "Open Field"))), 201, "create map");

        long gmTokenId = tokenIdByName(session.id(), gmJwt, "b3gm");
        long playerTokenId = tokenIdByName(session.id(), gmJwt, "b3pip");

        JsonNode gmPlace = Api.json(Api.post(baseUrl() + "/api/sessions/" + session.id()
                + "/map/tokens/" + gmTokenId + "/place", gmJwt, Api.body(Map.of("x", 20, "y", 15))).body());
        assertThat(gmPlace.at("/tokens/0/posX").asInt()).isEqualTo(20);
        assertThat(gmPlace.at("/tokens/0/posY").asInt()).isEqualTo(15);
        assertThat(gmPlace.at("/tokens/0/movedFeet").asInt()).isZero();

        JsonNode playerPlace = Api.json(Api.post(baseUrl() + "/api/sessions/" + session.id()
                + "/map/tokens/" + playerTokenId + "/place", playerJwt, Api.body(Map.of("x", 3, "y", 16))).body());
        assertThat(playerPlace.at("/tokens/1/posX").asInt()).isEqualTo(3);
        assertThat(playerPlace.at("/tokens/1/movedFeet").asInt()).isZero();

        Api.Response playerPlacesGmToken = Api.post(baseUrl() + "/api/sessions/" + session.id()
                + "/map/tokens/" + gmTokenId + "/place", playerJwt, Api.body(Map.of("x", 1, "y", 1)));
        assertThat(playerPlacesGmToken.status()).isEqualTo(403);

        Api.requireStatus(Api.post(baseUrl() + "/api/sessions/" + session.id() + "/map/initiative", gmJwt,
                Api.body(Map.of("entries", List.of(
                        Map.of("tokenId", gmTokenId, "score", 15),
                        Map.of("tokenId", playerTokenId, "score", 10))))), 200, "set initiative");
        Api.requireStatus(Api.post(baseUrl() + "/api/sessions/" + session.id() + "/map/initiative/next",
                gmJwt, null), 200, "start combat");

        Api.Response placedInCombat = Api.post(baseUrl() + "/api/sessions/" + session.id()
                + "/map/tokens/" + playerTokenId + "/place", playerJwt, Api.body(Map.of("x", 10, "y", 10)));
        assertThat(placedInCombat.status()).isEqualTo(400);
        assertThat(Api.json(placedInCombat.body()).at("/message").asText()).contains("use move instead");
    }

    @Test
    void playerTokenCarriesLinkedCharacterAvatar() throws Exception {
        String gmJwt = registerVerifyLogin("b4gm");
        String playerJwt = registerVerifyLogin("b4pip");
        SessionHandle session = createSession(gmJwt, "Avatar Hall");
        joinSession(playerJwt, session.inviteCode());

        Api.requireStatus(Api.post(baseUrl() + "/api/users/me/characters", playerJwt, Api.body(Map.ofEntries(
                Map.entry("name", "Tordek"),
                Map.entry("strength", 15), Map.entry("dexterity", 14),
                Map.entry("constitution", 15), Map.entry("intelligence", 12),
                Map.entry("wisdom", 10), Map.entry("charisma", 8),
                Map.entry("scoreSource", "STANDARD_ARRAY"),
                Map.entry("startingLevel", 1),
                Map.entry("sex", "MALE"),
                Map.entry("appearance", Map.of("skinTone", 120, "hairTone", 300, "hairStyle", "ponytail")),
                Map.entry("raceIndex", "dwarf"),
                Map.entry("classIndex", "cleric"),
                Map.entry("subclassIndex", "life"),
                Map.entry("backgroundIndex", "acolyte"),
                Map.entry("skillPickIndexes", Set.of("medicine", "religion")),
                Map.entry("spellIndexes", Set.of("guidance", "sacred-flame", "cure-wounds")),
                Map.entry("equipmentIndexes", Set.of("leather-armor", "shield"))))), 201, "create avatar character");

        Api.requireStatus(Api.post(baseUrl() + "/api/sessions/" + session.id() + "/map", gmJwt,
                Api.body(Map.of("name", "Avatar Hall"))), 201, "create map");

        JsonNode playerToken = first(get(session.id(), playerJwt), "b4pip");
        assertThat(playerToken.get("raceIndex").asText()).isEqualTo("dwarf");
        assertThat(playerToken.get("sex").asText()).isEqualTo("MALE");
        assertThat(playerToken.get("appearance").get("skinTone").asInt()).isEqualTo(120);
        assertThat(playerToken.get("appearance").get("hairTone").asInt()).isEqualTo(300);
        assertThat(playerToken.get("appearance").get("hairStyle").asText()).isEqualTo("ponytail");

        JsonNode gmToken = first(get(session.id(), gmJwt), "b4gm");
        assertThat(gmToken.get("raceIndex").isNull()).isTrue();
        assertThat(gmToken.get("sex").isNull()).isTrue();
        assertThat(gmToken.get("appearance").isNull()).isTrue();
    }

    @Test
    void mapBuilderTileJourney() throws Exception {
        String gmJwt = registerVerifyLogin("b5gm");
        String playerJwt = registerVerifyLogin("b5pip");
        SessionHandle session = createSession(gmJwt, "Mosaic Hall");
        joinSession(playerJwt, session.inviteCode());

        Api.requireStatus(Api.post(baseUrl() + "/api/sessions/" + session.id() + "/map", gmJwt,
                Api.body(Map.of("name", "Mosaic Hall"))), 201, "create map");

        JsonNode catalog = Api.json(Api.get(baseUrl() + "/api/sessions/" + session.id()
                + "/map/catalog", gmJwt).body());
        assertThat(catalog.get("tiles").size()).isGreaterThan(0);
        assertThat(catalog.at("/tiles/0/index").asInt()).isZero();
        assertThat(catalog.at("/tiles/0/name").asText()).isNotBlank();
        assertThat(catalog.at("/tiles/0/color").asText()).startsWith("#");

        JsonNode painted = Api.json(Api.put(baseUrl() + "/api/sessions/" + session.id() + "/map/tiles", gmJwt,
                Api.body(Map.of("tiles", List.of(
                        Map.of("x", 1, "y", 1, "tileIndex", 0),
                        Map.of("x", 2, "y", 1, "tileIndex", 7))))).body());
        assertThat(painted.get("tiles").size()).isEqualTo(2);
        assertThat(painted.at("/tiles/0/tileIndex").asInt()).isZero();
        assertThat(painted.at("/tiles/1/tileIndex").asInt()).isEqualTo(7);

        JsonNode erased = Api.json(Api.put(baseUrl() + "/api/sessions/" + session.id() + "/map/tiles", gmJwt,
                Api.body(Map.of("tiles", List.of(Map.of("x", 1, "y", 1))))).body());
        assertThat(erased.get("tiles").size()).isEqualTo(1);
        assertThat(erased.at("/tiles/0/tileIndex").asInt()).isEqualTo(7);

        Api.Response playerPaint = Api.put(baseUrl() + "/api/sessions/" + session.id() + "/map/tiles", playerJwt,
                Api.body(Map.of("tiles", List.of(Map.of("x", 3, "y", 1, "tileIndex", 0)))));
        assertThat(playerPaint.status()).isEqualTo(403);

        Api.Response unknownTile = Api.put(baseUrl() + "/api/sessions/" + session.id() + "/map/tiles", gmJwt,
                Api.body(Map.of("tiles", List.of(Map.of("x", 3, "y", 1, "tileIndex", 999)))));
        assertThat(unknownTile.status()).isEqualTo(400);

        Api.requireStatus(Api.patch(baseUrl() + "/api/sessions/" + session.id() + "/map", gmJwt,
                Api.body(Map.of("width", 2, "height", 2))), 200, "shrink map");
        assertThat(get(session.id(), gmJwt).get("tiles").size()).isZero();
    }

    private JsonNode get(long sessionId, String jwt) {
        Api.Response response = Api.get(baseUrl() + "/api/sessions/" + sessionId + "/map", jwt);
        return Api.json(response.body());
    }

    private JsonNode moveToken(long sessionId, String jwt, long tokenId, int x, int y) {
        Api.Response response = Api.post(baseUrl() + "/api/sessions/" + sessionId
                + "/map/tokens/" + tokenId + "/move", jwt, Api.body(Map.of("x", x, "y", y)));
        Api.requireStatus(response, 200, "move token " + tokenId);
        return Api.json(response.body());
    }

    private long tokenIdByName(long sessionId, String jwt, String name) {
        return first(get(sessionId, jwt), name).get("id").asLong();
    }

    private JsonNode first(JsonNode map, String name) {
        for (JsonNode token : map.get("tokens")) {
            if (name.equals(token.get("name").asText())) {
                return token;
            }
        }
        throw new AssertionError("token named '" + name + "' not found on map");
    }
}