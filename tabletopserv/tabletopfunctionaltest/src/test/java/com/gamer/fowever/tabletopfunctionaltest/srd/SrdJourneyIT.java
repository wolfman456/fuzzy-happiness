package com.gamer.fowever.tabletopfunctionaltest.srd;

import com.gamer.fowever.tabletopfunctionaltest.FunctionalTestBase;
import com.gamer.fowever.tabletopfunctionaltest.support.Api;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SRD journeys on the packaged backend: list and detail requests flow through the
 * real GatewayClient to the recorded-fixture gateway stub, so they never touch the
 * internet and stay deterministic.
 */
class SrdJourneyIT extends FunctionalTestBase {

    @Test
    void listAndDetailServeRecordedFixtures() throws Exception {
        String jwt = registerVerifyLogin("srd1");

        Api.Response list = Api.get(baseUrl() + "/api/srd/ability-scores", jwt);
        Api.requireStatus(list, 200, "srd list");
        JsonNode listBody = Api.json(list.body());
        assertThat(listBody.get("fixture").asBoolean()).isTrue();
        assertThat(listBody.get("count").asInt()).isEqualTo(6);
        assertThat(listBody.at("/results/0/index").asText()).isEqualTo("str");

        Api.Response detail = Api.get(baseUrl() + "/api/srd/ability-scores/str", jwt);
        Api.requireStatus(detail, 200, "srd detail");
        JsonNode detailBody = Api.json(detail.body());
        assertThat(detailBody.get("fixture").asBoolean()).isTrue();
        assertThat(detailBody.get("index").asText()).isEqualTo("str");
        assertThat(detailBody.get("full_name").asText()).isEqualTo("Strength");
    }

    @Test
    void unknownCollectionIsRejectedByBackend() throws Exception {
        String jwt = registerVerifyLogin("srd2");
        assertThat(Api.get(baseUrl() + "/api/srd/bogus", jwt).status())
                .as("unknown SRD collection is rejected before any upstream call").isEqualTo(400);
    }

    @Test
    void srdRequiresAuthentication() {
        assertThat(Api.get(baseUrl() + "/api/srd/ability-scores", null).status()).isEqualTo(401);
    }
}