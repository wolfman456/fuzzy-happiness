package com.gamer.fowever.tabletopfunctionaltest.character;

import com.gamer.fowever.tabletopfunctionaltest.FunctionalTestBase;
import com.gamer.fowever.tabletopfunctionaltest.support.Api;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Character-creation journeys on the packaged backend: the SRD allow-lists come from
 * the recorded fixtures, so compile/generate/create stay deterministic and offline.
 */
class CharacterJourneyIT extends FunctionalTestBase {

    private static Map<String, Object> legalDraft() {
        return Map.ofEntries(
                Map.entry("name", "Tordek"),
                Map.entry("strength", 15), Map.entry("dexterity", 14),
                Map.entry("constitution", 15), Map.entry("intelligence", 12),
                Map.entry("wisdom", 10), Map.entry("charisma", 8),
                Map.entry("scoreSource", "STANDARD_ARRAY"),
                Map.entry("startingLevel", 1),
                Map.entry("raceIndex", "dwarf"),
                Map.entry("classIndex", "cleric"),
                Map.entry("subclassIndex", "life"),
                Map.entry("backgroundIndex", "acolyte"),
                Map.entry("skillPickIndexes", Set.of("medicine", "religion")),
                Map.entry("spellIndexes", Set.of("sacred-flame", "cure-wounds")),
                Map.entry("equipmentIndexes", Set.of("leather-armor", "shield")));
    }

    @Test
    void compileDerivesSheetFromLegalDraft() throws Exception {
        String jwt = registerVerifyLogin("char1");

        Api.Response compile = Api.post(baseUrl() + "/api/characters/compile", jwt, Api.body(legalDraft()));
        Api.requireStatus(compile, 200, "compile legal draft");
        JsonNode body = Api.json(compile.body());
        assertThat(body.get("valid").asBoolean()).isTrue();
        assertThat(body.get("violations")).isEmpty();
        assertThat(body.get("sheet").isNull()).isFalse();

        JsonNode sheet = body.get("sheet");
        assertThat(sheet.get("name").asText()).isEqualTo("Tordek");
        assertThat(sheet.get("level").asInt()).isEqualTo(1);
        assertThat(sheet.get("hitPoints").asInt()).isEqualTo(10);
        assertThat(sheet.get("armorClass").asInt()).isEqualTo(15);
        assertThat(sheet.get("speedFeet").asInt()).isEqualTo(25);
        assertThat(sheet.get("proficiencyBonus").asInt()).isEqualTo(2);
        assertThat(sheet.get("savingThrows")).hasSize(2);
        assertThat(sheet.get("spellSlots").get("1").asInt()).isEqualTo(2);
        assertThat(sheet.get("spellSlots").get("0").asInt()).isEqualTo(3);
        assertThat(sheet.get("featureIndexes")).isNotEmpty();
        assertThat(sheet.get("sheetSnapshot").isNull()).isFalse();
    }

    @Test
    void compileReportsViolationsForIllegalScores() throws Exception {
        String jwt = registerVerifyLogin("char2");

        Map<String, Object> bad = Map.ofEntries(
                Map.entry("name", "Tordek"),
                Map.entry("strength", 18), Map.entry("dexterity", 14),
                Map.entry("constitution", 15), Map.entry("intelligence", 12),
                Map.entry("wisdom", 10), Map.entry("charisma", 8),
                Map.entry("scoreSource", "STANDARD_ARRAY"),
                Map.entry("startingLevel", 1),
                Map.entry("raceIndex", "dwarf"), Map.entry("classIndex", "cleric"),
                Map.entry("subclassIndex", "life"), Map.entry("backgroundIndex", "acolyte"),
                Map.entry("skillPickIndexes", Set.of("medicine", "religion")),
                Map.entry("spellIndexes", Set.of("sacred-flame", "cure-wounds")),
                Map.entry("equipmentIndexes", Set.of("leather-armor", "shield")));

        JsonNode body = Api.json(Api.post(baseUrl() + "/api/characters/compile", jwt, Api.body(bad)).body());
        assertThat(body.get("valid").asBoolean()).isFalse();
        assertThat(body.get("sheet").isNull()).isTrue();
        assertThat(body.get("violations")).anyMatch(v -> v.asText().contains("scores"));
    }

    @Test
    void generateBuildsALegalQuickBuild() throws Exception {
        String jwt = registerVerifyLogin("char3");

        JsonNode body = Api.json(Api.post(baseUrl() + "/api/characters/generate", jwt,
                Api.body(Map.of("name", "Automaton", "scoreSource", "STANDARD_ARRAY",
                        "startingLevel", 1, "seed", 42))).body());

        assertThat(body.get("valid").asBoolean()).isTrue();
        JsonNode sheet = body.get("sheet");
        assertThat(sheet.get("name").asText()).isEqualTo("Automaton");
        assertThat(sheet.get("raceIndex").asText()).isEqualTo("dwarf");
        assertThat(sheet.get("classIndex").asText()).isEqualTo("cleric");
        assertThat(sheet.get("backgroundIndex").asText()).isEqualTo("acolyte");
        assertThat(sheet.get("strength").asInt()).isBetween(3, 18);
        assertThat(sheet.get("spellIndexes")).isNotEmpty();
    }

    @Test
    void createPersistsMineListsAndGetRePairsId() throws Exception {
        String jwt = registerVerifyLogin("char4");
        String otherJwt = registerVerifyLogin("char4b");

        Api.Response created = Api.post(baseUrl() + "/api/users/me/characters", jwt, Api.body(legalDraft()));
        Api.requireStatus(created, 201, "create character");
        JsonNode createdBody = Api.json(created.body());
        long id = createdBody.get("id").asLong();
        assertThat(id).isPositive();
        assertThat(createdBody.get("name").asText()).isEqualTo("Tordek");

        JsonNode mine = Api.json(Api.get(baseUrl() + "/api/users/me/characters", jwt).body());
        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).get("id").asLong()).isEqualTo(id);
        assertThat(mine.get(0).get("subclassIndex").asText()).isEqualTo("life");

        JsonNode fetched = Api.json(Api.get(baseUrl() + "/api/users/me/characters/" + id, jwt).body());
        assertThat(fetched.get("id").asLong()).isEqualTo(id);
        assertThat(fetched.get("name").asText()).isEqualTo("Tordek");
        assertThat(fetched.get("sheetSnapshot").get("name").asText()).isEqualTo("Tordek");

        JsonNode otherMine = Api.json(Api.get(baseUrl() + "/api/users/me/characters", otherJwt).body());
        assertThat(otherMine).isEmpty();

        assertThat(Api.get(baseUrl() + "/api/users/me/characters/" + id, otherJwt).status())
                .as("another user cannot fetch the character").isEqualTo(404);
    }

    @Test
    void createRejectsIllegalDraftWithBadRequest() throws Exception {
        String jwt = registerVerifyLogin("char5");

        Api.Response created = Api.post(baseUrl() + "/api/users/me/characters", jwt, Api.body(Map.ofEntries(
                Map.entry("name", "Tordek"),
                Map.entry("strength", 18), Map.entry("dexterity", 14),
                Map.entry("constitution", 15), Map.entry("intelligence", 12),
                Map.entry("wisdom", 10), Map.entry("charisma", 8),
                Map.entry("scoreSource", "STANDARD_ARRAY"),
                Map.entry("startingLevel", 1),
                Map.entry("raceIndex", "dwarf"), Map.entry("classIndex", "cleric"),
                Map.entry("subclassIndex", "life"), Map.entry("backgroundIndex", "acolyte"),
                Map.entry("skillPickIndexes", List.of()), Map.entry("spellIndexes", List.of()),
                Map.entry("equipmentIndexes", List.of()))));

        assertThat(created.status()).isEqualTo(400);
    }

    @Test
    void characterEndpointsRequireAuthentication() {
        assertThat(Api.post(baseUrl() + "/api/characters/compile", null, Api.body(legalDraft())).status())
                .isEqualTo(401);
        assertThat(Api.get(baseUrl() + "/api/users/me/characters", null).status()).isEqualTo(401);
    }
}