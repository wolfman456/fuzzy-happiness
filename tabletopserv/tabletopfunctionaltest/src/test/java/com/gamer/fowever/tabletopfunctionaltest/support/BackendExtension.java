package com.gamer.fowever.tabletopfunctionaltest.support;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Boots a single {@link BackendInstance} once per functional-test fork JVM. All *IT
 * classes share it (failsafe default: one JVM, reused forks), which keeps DB/schema
 * setup cost at one cold boot. The instance is released by a JVM shutdown hook.
 */
public final class BackendExtension implements BeforeAllCallback {

    private static volatile BackendInstance instance;

    public static BackendInstance backend() {
        BackendInstance current = instance;
        if (current == null) {
            throw new IllegalStateException("backend not started — did the BackendExtension run?");
        }
        return current;
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (instance == null) {
            String jarPath = System.getProperty("tabletopserv.functional.jar");
            if (jarPath == null || jarPath.isBlank()) {
                throw new IllegalStateException(
                        "System property tabletopserv.functional.jar is required (set by maven-failsafe-plugin).");
            }
            if (!Files.isRegularFile(Path.of(jarPath))) {
                throw new IllegalStateException(
                        "No runnable service JAR at " + jarPath + " — run `./mvnw -pl tabletopservice -am package` first.");
            }
            instance = BackendInstance.start(jarPath,
                    System.getProperty("tabletopserv.functional.db-url"),
                    System.getProperty("tabletopserv.functional.db-user"),
                    System.getProperty("tabletopserv.functional.db-password"));
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                BackendInstance current = instance;
                if (current != null) {
                    current.close();
                }
            }, "backend-instance-shutdown"));
        }
    }
}