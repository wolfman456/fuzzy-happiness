package com.gamer.fowever.tabletopfunctionaltest.monster;

import com.gamer.fowever.tabletopfunctionaltest.FunctionalTestBase;
import com.gamer.fowever.tabletopfunctionaltest.support.Api;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Monster journeys on the packaged backend: generation persists to the actor's own
 * collection so a monster is generated once and reused later.
 */
class MonsterJourneyIT extends FunctionalTestBase {

    @Test
    void generatePersistsAndListsOwnedMonsters() throws Exception {
        String gmJwt = registerVerifyLogin("m1gm");

        JsonNode generated = Api.json(Api.post(baseUrl() + "/api/monsters/generate", gmJwt,
                Api.body(Map.of("cr", "1/4", "role", "BRUTE", "name", "Grimmjaw", "seed", 42))).body());
        assertThat(generated.get("name").asText()).isEqualTo("Grimmjaw");
        assertThat(generated.get("cr").asText()).isEqualTo("1/4");
        assertThat(generated.get("role").asText()).isEqualTo("BRUTE");
        assertThat(generated.get("hitPoints").asInt()).isPositive();
        assertThat(generated.get("actions").size()).isPositive();

        JsonNode once = Api.json(Api.get(baseUrl() + "/api/monsters/mine", gmJwt).body());
        assertThat(once).as("first generation persisted to mine").hasSize(1);

        Api.requireStatus(Api.post(baseUrl() + "/api/monsters/generate", gmJwt,
                Api.body(Map.of("cr", "1/4", "name", "Grimmjaw", "seed", 7))), 201, "second generation");

        JsonNode twice = Api.json(Api.get(baseUrl() + "/api/monsters/mine", gmJwt).body());
        assertThat(twice).hasSize(2);
        assertThat(twice.get(0).get("name").asText())
                .as("mine orders newest first for reuse").isEqualTo("Grimmjaw");
    }

    @Test
    void generationIsScopedToTheActor() throws Exception {
        String gmJwt = registerVerifyLogin("m2gm");
        Api.requireStatus(Api.post(baseUrl() + "/api/monsters/generate", gmJwt,
                Api.body(Map.of("cr", "1", "name", "Watchdrake", "seed", 1))), 201, "generate for gm");

        String otherJwt = registerVerifyLogin("m2other");
        JsonNode other = Api.json(Api.get(baseUrl() + "/api/monsters/mine", otherJwt).body());
        assertThat(other).isEmpty();
    }

    @Test
    void generateRejectsUnsupportedChallengeRating() throws Exception {
        String gmJwt = registerVerifyLogin("m3gm");
        Api.Response bad = Api.post(baseUrl() + "/api/monsters/generate", gmJwt,
                Api.body(Map.of("cr", "not-a-cr")));
        assertThat(bad.status()).isEqualTo(400);
    }
}