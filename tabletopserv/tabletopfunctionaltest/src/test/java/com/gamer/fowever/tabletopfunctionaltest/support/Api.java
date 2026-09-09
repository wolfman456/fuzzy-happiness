package com.gamer.fowever.tabletopfunctionaltest.support;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Minimal JSON-over-HTTP client for driving the backend under test through its real
 * interface. Token-carrying helpers send {@code Authorization: Bearer <jwt>}.
 */
public final class Api {

    public static final ObjectMapper MAPPER = new ObjectMapper();

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public record Response(int status, String body) {
    }

    private Api() {
    }

    public static Response get(String url, String bearerToken) {
        return send("GET", url, bearerToken, null);
    }

    public static Response post(String url, String bearerToken, String jsonBody) {
        return send("POST", url, bearerToken, jsonBody);
    }

    public static Response patch(String url, String bearerToken, String jsonBody) {
        return send("PATCH", url, bearerToken, jsonBody);
    }

    public static Response delete(String url, String bearerToken) {
        return send("DELETE", url, bearerToken, null);
    }

    public static JsonNode json(String body) {
        try {
            return MAPPER.readTree(body);
        } catch (JacksonException ex) {
            throw new IllegalArgumentException("response was not valid JSON: " + body, ex);
        }
    }

    public static String body(Object payload) {
        try {
            return MAPPER.writeValueAsString(payload);
        } catch (JacksonException ex) {
            throw new IllegalArgumentException("cannot serialize request payload " + payload, ex);
        }
    }

    public static void requireStatus(Response response, int expected, String context) {
        if (response.status() != expected) {
            throw new AssertionError(context + ": expected HTTP " + expected + " but got " + response.status()
                    + " — body: " + response.body());
        }
    }

    private static Response send(String method, String url, String bearerToken, String jsonBody) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .method(method, bodyPublisher(jsonBody));
        if (bearerToken != null) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        if (jsonBody != null) {
            builder.header("Content-Type", "application/json");
        }
        try {
            HttpResponse<String> response = CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new Response(response.statusCode(), response.body());
        } catch (IOException | InterruptedException ex) {
            throw new IllegalStateException("HTTP " + method + " " + url + " failed", ex);
        }
    }

    private static HttpRequest.BodyPublisher bodyPublisher(String jsonBody) {
        return jsonBody != null
                ? HttpRequest.BodyPublishers.ofString(jsonBody)
                : HttpRequest.BodyPublishers.noBody();
    }
}