package com.gamer.fowever.tabletopservice.service;

import com.gamer.fowever.tabletopapi.support.ApiException;
import com.gamer.fowever.tabletopservice.gateway.GatewayClient;
import com.gamer.fowever.tabletopservice.gateway.GatewayClientException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SrdClientTest {

    @Mock
    private GatewayClient gatewayClient;

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    @Test
    void listForwardsAllowlistedCollectionAndCuratesParams() {
        SrdClient client = new SrdClient(gatewayClient, objectMapper);
        when(gatewayClient.get("/api/srd/spells",
                Map.of("level", "1", "school", "evocation")))
                .thenReturn("{\"count\":1}");

        JsonNode result = client.list("spells", Map.of("level", "1", "school", "evocation", "hacker", "x"));

        assertThat(result.get("count").asInt()).isEqualTo(1);
        verify(gatewayClient).get(eq("/api/srd/spells"), eq(Map.of("level", "1", "school", "evocation")));
    }

    @Test
    void listRejectsUnknownCollection() {
        SrdClient client = new SrdClient(gatewayClient, objectMapper);

        assertThatThrownBy(() -> client.list("star-wars-cantina", Map.of()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("unknown SRD collection");
    }

    @Test
    void detailForwardsIndexPath() {
        SrdClient client = new SrdClient(gatewayClient, objectMapper);
        when(gatewayClient.get("/api/srd/races/dragonborn", Map.of()))
                .thenReturn("{\"name\":\"Dragonborn\"}");

        JsonNode result = client.detail("races", "dragonborn");

        assertThat(result.get("name").asText()).isEqualTo("Dragonborn");
        verify(gatewayClient).get("/api/srd/races/dragonborn", Map.of());
    }

    @Test
    void detailRejectsUnknownCollection() {
        SrdClient client = new SrdClient(gatewayClient, objectMapper);

        assertThatThrownBy(() -> client.detail("cantina", "yoda"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("unknown SRD collection");
    }

    @Test
    void upstreamServerErrorMapsToRulesDataUnavailable() {
        SrdClient client = new SrdClient(gatewayClient, objectMapper);
        when(gatewayClient.get("/api/srd/races", Map.of()))
                .thenThrow(new GatewayClientException(502, "upstream unavailable: srd"));

        assertThatThrownBy(() -> client.list("races", Map.of()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getStatus().value()).isEqualTo(502);
                    assertThat(api.getMessage()).isEqualTo("rules data unavailable");
                });
    }

    @Test
    void clientErrorFromGatewayMapsToSameStatusAndMessage() {
        SrdClient client = new SrdClient(gatewayClient, objectMapper);
        when(gatewayClient.get("/api/srd/races", Map.of()))
                .thenThrow(new GatewayClientException(404, "collection not found"));

        assertThatThrownBy(() -> client.list("races", Map.of()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getStatus().value()).isEqualTo(404);
                    assertThat(api.getMessage()).isEqualTo("collection not found");
                });
    }

    @Test
    void unparseableBodyMapsToBadGateway() {
        SrdClient client = new SrdClient(gatewayClient, objectMapper);
        when(gatewayClient.get("/api/srd/races", Map.of())).thenReturn("not json at all");

        assertThatThrownBy(() -> client.list("races", Map.of()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("rules data unavailable");
    }

    @Test
    void subresourceForwardsNestedIndexPath() {
        SrdClient client = new SrdClient(gatewayClient, objectMapper);
        when(gatewayClient.get("/api/srd/classes/cleric/levels", Map.of()))
                .thenReturn("[{\"level\":1}]");

        JsonNode result = client.subresource("classes", "cleric", "levels", Map.of());

        assertThat(result.isArray()).isTrue();
        verify(gatewayClient).get("/api/srd/classes/cleric/levels", Map.of());
    }

    @Test
    void subresourceRejectsUnknownSubresource() {
        SrdClient client = new SrdClient(gatewayClient, objectMapper);

        assertThatThrownBy(() -> client.subresource("classes", "cleric", "proficiencies", Map.of()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("unknown SRD subresource");
        assertThatThrownBy(() -> client.subresource("races", "dwarf", "spells", Map.of()))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void subresourceCuratesParams() {
        SrdClient client = new SrdClient(gatewayClient, objectMapper);
        when(gatewayClient.get("/api/srd/classes/cleric/spells",
                Map.of("level", "0"))).thenReturn("{\"count\":7}");

        JsonNode result = client.subresource("classes", "cleric", "spells",
                Map.of("level", "0", "hacker", "x"));

        assertThat(result.get("count").asInt()).isEqualTo(7);
        verify(gatewayClient).get(eq("/api/srd/classes/cleric/spells"), eq(Map.of("level", "0")));
    }
}