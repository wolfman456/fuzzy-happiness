package com.gamer.fowever.tabletopfunctionaltest.migration;

import com.gamer.fowever.tabletopfunctionaltest.support.Api;
import com.gamer.fowever.tabletopfunctionaltest.support.BackendInstance;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.DockerClientFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Proves the Flyway baseline (design-draft-v2.md §3) on the real prod path: an empty
 * PostgreSQL database is migrated by Flyway (V1__baseline.sql) and then Hibernate only
 * validates the result (ddl-auto=validate). If the baseline did not match the entity
 * model, the service subprocess would fail to start and this IT would fail with the
 * child log.
 *
 * <p>PostgreSQL-only: the baseline is PostgreSQL SQL (e.g. {@code oid} columns). The Postgres
 * it runs against is, in order of preference: an explicit
 * {@code -Dtabletopserv.functional.db-url=jdbc:postgresql:...} database, a Testcontainers
 * container (CI, Docker available), or a rootless embedded Postgres binary
 * ({@code io.zonky.test}) — so the baseline is still exercised locally without Docker or
 * admin rights (the H2 escape hatch used by the journey suites does not apply here).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FlywayMigrationIT {

    private static final String PASSWORD = "Password1!";

    private EmbeddedPostgres embeddedPostgres;
    private BackendInstance backend;

    @BeforeAll
    void bootMigratedBackend() throws Exception {
        String jarPath = System.getProperty("tabletopserv.functional.jar");
        assumeTrue(jarPath != null && Files.isRegularFile(Path.of(jarPath)),
                "service JAR not built; skipping Flyway migration IT");

        String externalDbUrl = System.getProperty("tabletopserv.functional.db-url");
        String dbUrl = null;
        String dbUser = null;
        String dbPassword = null;

        if (externalDbUrl != null && externalDbUrl.startsWith("jdbc:postgresql")) {
            dbUrl = externalDbUrl;
            dbUser = System.getProperty("tabletopserv.functional.db-user");
            dbPassword = System.getProperty("tabletopserv.functional.db-password");
        } else if (!DockerClientFactory.instance().isDockerAvailable()) {
            // No Docker (and no explicit Postgres): run against a rootless embedded binary so
            // the baseline is still validated locally. The journey suites' H2 escape hatch does
            // not apply here because the baseline is PostgreSQL-specific.
            embeddedPostgres = EmbeddedPostgres.builder().start();
            dbUrl = "jdbc:postgresql://localhost:" + embeddedPostgres.getPort() + "/postgres";
            dbUser = "postgres";
            dbPassword = "postgres";
        }

        backend = BackendInstance.startMigrating(jarPath, dbUrl, dbUser, dbPassword);
    }

    @AfterAll
    void stop() {
        if (backend != null) {
            backend.close();
        }
        if (embeddedPostgres != null) {
            try {
                embeddedPostgres.close();
            } catch (Exception ignored) {
                // best-effort shutdown of the throwaway Postgres
            }
        }
    }

    @Test
    void flywayBaselineBootsValidatesAndServesSeedData() throws Exception {
        assertNotNull(backend, "migrated backend should have started");
        assertTrue(backend.isHealthy(), "backend should be healthy after Flyway migrate + Hibernate validate");

        String username = "flyway_probe";
        String email = username + "@example.com";
        Api.requireStatus(Api.post(backend.baseUrl() + "/api/auth/register", null,
                Api.body(Map.of("displayName", username, "realName", "Flyway Probe", "email", email,
                        "dateOfBirth", "1990-01-15", "username", username,
                        "password", PASSWORD, "confirmPassword", PASSWORD))),
                201, "register against migrated schema");

        Api.Response login = Api.post(backend.baseUrl() + "/api/auth/login", null,
                Api.body(Map.of("identifier", username, "password", PASSWORD)));
        Api.requireStatus(login, 200, "login against migrated schema");
        String jwt = Api.json(login.body()).get("token").asText();

        Api.Response games = Api.get(backend.baseUrl() + "/api/games", jwt);
        Api.requireStatus(games, 200, "list games seeded over migrated schema");
        assertTrue(games.body().contains("dnd-5e"),
                "seeded dnd-5e game should be present after migrating the baseline");
    }
}
