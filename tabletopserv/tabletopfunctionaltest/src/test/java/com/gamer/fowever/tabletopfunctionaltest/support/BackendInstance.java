package com.gamer.fowever.tabletopfunctionaltest.support;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs the packaged tabletopservice JAR as a subprocess against a throwaway Postgres
 * (Testcontainers) and a recorded-fixture gateway stub, so the functional suites drive
 * the real, production-shaped service over its actual HTTP/WebSocket interface.
 *
 * <p>The dev profile keeps the console email sender and the bootstrap runners active:
 * email verification links are parsed out of the child JVM log by email address.
 */
public final class BackendInstance implements AutoCloseable {

    private static final Pattern VERIFY_LINE = Pattern.compile(
            "DEV EMAIL to \\[([^\\]]+)\\] .*?verify: (\\S+)");
    private static final long HEALTH_TIMEOUT_MS = 180_000;
    private static final long TOKEN_TIMEOUT_MS = 30_000;
    private static final int LOG_TAIL_LIMIT = 5_000;

    private final Path jar;
    private final int appPort;
    private final String baseUrl;
    private final String externalDbUrl;
    private final String externalDbUser;
    private final String externalDbPassword;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private PostgreSQLContainer<?> postgres;
    private FixtureGateway gateway;
    private Process process;
    private Thread logPump;
    private final Map<String, String> verificationUrls = new ConcurrentHashMap<>();
    private final List<String> logTail = new CopyOnWriteArrayList<>();

    private BackendInstance(Path jar, String externalDbUrl, String externalDbUser, String externalDbPassword) {
        appPort = findFreePort();
        this.jar = jar;
        this.externalDbUrl = externalDbUrl;
        this.externalDbUser = externalDbUser;
        this.externalDbPassword = externalDbPassword;
        baseUrl = "http://127.0.0.1:" + appPort;
    }

    /**
     * @param externalDbUrl optional JDBC URL overriding the Testcontainers Postgres
     *                      (used when Docker is unavailable, e.g. constrained local
     *                      dev); CI always runs the Testcontainers path.
     */
    public static BackendInstance start(String jarPath, String externalDbUrl,
                                        String externalDbUser, String externalDbPassword) throws Exception {
        Path jar = Path.of(jarPath);
        if (!Files.isRegularFile(jar) || !Files.isReadable(jar)) {
            throw new IllegalStateException(
                    "Runnable service JAR not found at " + jarPath + ".\n"
                            + "Build it first (from tabletopserv/) with: ./mvnw -pl tabletopservice -am package");
        }
        BackendInstance instance = new BackendInstance(jar, externalDbUrl, externalDbUser, externalDbPassword);
        instance.boot();
        return instance;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public FixtureGateway gateway() {
        return gateway;
    }

    public String awaitVerificationUrl(String email) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TOKEN_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline && !verificationUrls.containsKey(email)) {
            Thread.sleep(50);
        }
        return verificationUrls.get(email);
    }

    public String tailLog(int lines) {
        int from = Math.max(0, logTail.size() - lines);
        return String.join(System.lineSeparator(), logTail.subList(from, logTail.size()));
    }

    private void boot() throws Exception {
        if (externalDbUrl == null || externalDbUrl.isBlank()) {
            postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("tabletop")
                    .withUsername("tabletop")
                    .withPassword("tabletop");
            postgres.start();
        }
        gateway = new FixtureGateway();
        startServiceProcess();
        waitForHealth();
    }

    private void startServiceProcess() throws IOException {
        String javaBinary = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        ProcessBuilder pb = new ProcessBuilder(javaBinary, "-jar", jar.toString());
        Map<String, String> env = pb.environment();
        env.put("SPRING_PROFILES_ACTIVE", "dev");
        env.put("SERVER_PORT", String.valueOf(appPort));
        if (postgres != null) {
            env.put("SPRING_DATASOURCE_URL", postgres.getJdbcUrl());
            env.put("SPRING_DATASOURCE_USERNAME", postgres.getUsername());
            env.put("SPRING_DATASOURCE_PASSWORD", postgres.getPassword());
        } else {
            env.put("SPRING_DATASOURCE_URL", externalDbUrl);
            env.put("SPRING_DATASOURCE_USERNAME", externalDbUser == null ? "sa" : externalDbUser);
            env.put("SPRING_DATASOURCE_PASSWORD", externalDbPassword == null ? "" : externalDbPassword);
        }
        env.put("SPRING_JPA_HIBERNATE_DDL_AUTO", "create-drop");
        env.put("SPRING_SQL_INIT_MODE", "never");
        env.put("TABLETOPSERV_GATEWAY_URL", gateway.baseUrl());
        env.put("TABLETOPSERV_GATEWAY_TOKEN", FixtureGateway.TOKEN);
        env.put("TABLETOPSERV_APP_BASE_URL", baseUrl);
        pb.redirectErrorStream(true);
        process = pb.start();
        logPump = new Thread(this::pumpLogs, "backend-log-pump");
        logPump.setDaemon(true);
        logPump.start();
    }

    private void pumpLogs() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                recordLogLine(line);
            }
        } catch (IOException ignored) {
            // process exited
        }
    }

    private void recordLogLine(String line) {
        synchronized (logTail) {
            logTail.add(line);
            if (logTail.size() > LOG_TAIL_LIMIT) {
                logTail.subList(0, logTail.size() - LOG_TAIL_LIMIT).clear();
            }
        }
        Matcher matcher = VERIFY_LINE.matcher(line);
        if (matcher.find()) {
            verificationUrls.put(matcher.group(1), matcher.group(2));
        }
    }

    private void waitForHealth() throws Exception {
        long deadline = System.currentTimeMillis() + HEALTH_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (process.isAlive() && isHealthy()) {
                return;
            }
            Thread.sleep(500);
        }
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }
        throw new IllegalStateException(
                "Backend subprocess did not become healthy within " + HEALTH_TIMEOUT_MS + " ms.\n"
                        + "--- child log (tail) ---\n" + tailLog(200));
    }

    private boolean isHealthy() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/actuator/health"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (IOException | InterruptedException ex) {
            return false;
        }
    }

    private static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException ex) {
            throw new IllegalStateException("no free port available", ex);
        }
    }

    @Override
    public void close() {
        if (process != null && process.isAlive()) {
            process.destroy();
            try {
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException ex) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
            }
        }
        if (logPump != null) {
            logPump.interrupt();
        }
        if (gateway != null) {
            gateway.close();
        }
        if (postgres != null) {
            postgres.stop();
        }
    }
}