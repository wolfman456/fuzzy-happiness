package com.gamer.fowever.tabletopservice.service;

import com.gamer.fowever.tabletopapi.support.ApiException;
import com.gamer.fowever.tabletopservice.gateway.GatewayClient;
import com.gamer.fowever.tabletopservice.gateway.GatewayClientException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Set;

@Service
public class SrdClient {

    private static final Set<String> COLLECTIONS = Set.of(
            "races", "classes", "subclasses", "subraces", "backgrounds", "ability-scores", "skills",
            "proficiencies", "equipment", "equipment-categories", "spells", "features",
            "traits", "feats", "conditions", "languages", "monsters");

    private static final Set<String> ALLOWED_PARAMS = Set.of("level", "school", "name", "index");

    private static final Map<String, Set<String>> SUBRESOURCES = Map.of(
            "classes", Set.of("spells", "levels"));

    private final GatewayClient gatewayClient;
    private final ObjectMapper objectMapper;

    public SrdClient(GatewayClient gatewayClient, ObjectMapper objectMapper) {
        this.gatewayClient = gatewayClient;
        this.objectMapper = objectMapper;
    }

    public JsonNode list(String collection, Map<String, String> params) {
        requireCollection(collection);
        Map<String, String> curated = curateParams(params);
        return fetch("/api/srd/" + collection, curated);
    }

    public JsonNode detail(String collection, String index) {
        requireCollection(collection);
        return fetch("/api/srd/" + collection + "/" + index, Map.of());
    }

    public JsonNode subresource(String collection, String index, String subresource, Map<String, String> params) {
        requireCollection(collection);
        if (!SUBRESOURCES.getOrDefault(collection, Set.of()).contains(subresource)) {
            throw ApiException.badRequest("unknown SRD subresource: " + collection + "/" + subresource);
        }
        return fetch("/api/srd/" + collection + "/" + index + "/" + subresource, curateParams(params));
    }

    private Map<String, String> curateParams(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> ALLOWED_PARAMS.contains(entry.getKey()))
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private JsonNode fetch(String path, Map<String, String> params) {
        String body;
        try {
            body = gatewayClient.get(path, params);
        } catch (GatewayClientException ex) {
            if (ex.getStatus() >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                throw ApiException.badGateway("rules data unavailable");
            }
            throw new ApiException(HttpStatus.valueOf(ex.getStatus()), ex.getMessage());
        }
        try {
            return objectMapper.readTree(body);
        } catch (JacksonException ex) {
            throw ApiException.badGateway("rules data unavailable");
        }
    }

    private void requireCollection(String collection) {
        if (!COLLECTIONS.contains(collection)) {
            throw ApiException.badRequest("unknown SRD collection: " + collection);
        }
    }
}