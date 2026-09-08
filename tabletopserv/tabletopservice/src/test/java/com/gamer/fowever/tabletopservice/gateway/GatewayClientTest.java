package com.gamer.fowever.tabletopservice.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void getReturnsBodyAndSendsTokenHeader() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/srd/races", exchange -> {
            String token = exchange.getRequestHeaders().getFirst("X-Gateway-Token");
            byte[] body = ("{\"token\":\"" + token + "\",\"count\":2}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();

        GatewayClient client = new GatewayClient(
                "http://127.0.0.1:" + server.getAddress().getPort(), "test-token");

        String body = client.get("/api/srd/races", Map.of());

        assertThat(body).contains("\"token\":\"test-token\"");
        assertThat(body).contains("\"count\":2");
    }

    @Test
    void gatewayErrorStatusThrowsGatewayClientException() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/srd/races", exchange -> {
            byte[] body = "{\"status\":403,\"message\":\"route not allowed\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(403, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();

        GatewayClient client = new GatewayClient(
                "http://127.0.0.1:" + server.getAddress().getPort(), "test-token");

        assertThatThrownBy(() -> client.get("/api/srd/races", Map.of()))
                .isInstanceOf(GatewayClientException.class)
                .satisfies(ex -> {
                    GatewayClientException gex = (GatewayClientException) ex;
                    assertThat(gex.getStatus()).isEqualTo(403);
                    assertThat(gex.getMessage()).isEqualTo("{\"status\":403,\"message\":\"route not allowed\"}");
                });
    }

    @Test
    void unreachableUpstreamMapsTo502() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        int port = server.getAddress().getPort();
        server.stop(0);
        server = null;

        GatewayClient client = new GatewayClient("http://127.0.0.1:" + port, "test-token");

        assertThatThrownBy(() -> client.get("/api/srd/races", Map.of()))
                .isInstanceOf(GatewayClientException.class)
                .satisfies(ex -> assertThat(((GatewayClientException) ex).getStatus()).isEqualTo(502));
    }

    @Test
    void queryParamsAreAppended() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/srd/spells", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            byte[] body = ("{\"query\":\"" + query + "\"}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();

        GatewayClient client = new GatewayClient(
                "http://127.0.0.1:" + server.getAddress().getPort(), "test-token");

        String body = client.get("/api/srd/spells", Map.of("level", "1", "school", "evocation"));

        assertThat(body).contains("level=1");
        assertThat(body).contains("school=evocation");
    }
}