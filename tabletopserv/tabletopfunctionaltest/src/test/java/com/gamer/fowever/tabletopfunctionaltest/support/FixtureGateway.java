package com.gamer.fowever.tabletopfunctionaltest.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Egress-gateway substitute that serves recorded SRD fixtures instead of proxying to
 * dnd5eapi.co. It mimics the real gateway's HTTP surface for the routes the backend
 * actually calls (X-Gateway-Token check on /api/srd/*), so the service under test
 * exercises its real GatewayClient contract without any internet access.
 */
public final class FixtureGateway implements AutoCloseable {

    public static final String TOKEN = "dev-gateway-token";

    private final HttpServer server;

    public FixtureGateway() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::route);
        server.start();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + port();
    }

    private void route(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            if (!"GET".equalsIgnoreCase(method)) {
                respond(exchange, 405, "{\"status\":405,\"message\":\"method not allowed\"}");
                return;
            }
            if (path.equals("/health")) {
                respond(exchange, 200, "{\"status\":\"UP\"}");
                return;
            }
            if (!TOKEN.equals(exchange.getRequestHeaders().getFirst("X-Gateway-Token"))) {
                respond(exchange, 403, "{\"status\":403,\"message\":\"missing or invalid gateway token\"}");
                return;
            }
            String fixture = loadFixture(path);
            if (fixture == null) {
                respond(exchange, 404, "{\"status\":404,\"message\":\"unknown upstream path\"}");
                return;
            }
            respond(exchange, 200, fixture);
        } finally {
            exchange.close();
        }
    }

    private String loadFixture(String path) {
        String resource;
        if (path.startsWith("/api/srd/")) {
            String rest = path.substring("/api/srd/".length());
            String[] parts = rest.split("/");
            resource = switch (parts.length) {
                case 1 -> "/fixtures/srd/" + parts[0] + ".json";
                case 2 -> "/fixtures/srd/" + parts[0] + "-" + parts[1] + ".json";
                case 3 -> "/fixtures/srd/" + parts[0] + "-" + parts[1] + "-" + parts[2] + ".json";
                default -> null;
            };
        } else {
            return null;
        }
        try (InputStream in = FixtureGateway.class.getResourceAsStream(resource)) {
            if (in == null) {
                return null;
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("failed reading fixture " + resource, ex);
        }
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}