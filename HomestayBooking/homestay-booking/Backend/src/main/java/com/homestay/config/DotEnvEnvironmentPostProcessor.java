package com.homestay.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads repo-root {@code .env} KEY=VALUE pairs into the Spring Environment early.
 * Spring's {@code optional:file:.env} often fails silently because {@code .env}
 * has no recognized extension — without this, MAIL_* fall back to yml defaults.
 */
public class DotEnvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "dotenv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            return;
        }

        Path envFile = resolveEnvFile();
        if (envFile == null) {
            return;
        }

        Map<String, Object> values = parseEnvFile(envFile);
        if (values.isEmpty()) {
            return;
        }

        // Highest precedence among local sources so MAIL_* override yml defaults
        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, values));
    }

    private static Path resolveEnvFile() {
        Path cwd = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        // IDE often starts with cwd = workspace root (HomestayBooking/) while .env lives under homestay-booking/
        java.util.LinkedHashSet<Path> candidates = new java.util.LinkedHashSet<>();
        Path dir = cwd;
        for (int i = 0; i < 5 && dir != null; i++) {
            candidates.add(dir.resolve(".env"));
            candidates.add(dir.resolve("backend").resolve(".env"));
            candidates.add(dir.resolve("homestay-booking").resolve(".env"));
            candidates.add(dir.resolve("homestay-booking").resolve("backend").resolve(".env"));
            dir = dir.getParent();
        }
        for (Path candidate : candidates) {
            if (candidate != null && Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static Map<String, Object> parseEnvFile(Path envFile) {
        Map<String, Object> values = new LinkedHashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(envFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                // Support optional "export KEY=VALUE"
                if (line.startsWith("export ")) {
                    line = line.substring(7).trim();
                }
                int eq = line.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                if ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                if (!key.isEmpty()) {
                    values.put(key, value);
                }
            }
        } catch (Exception ignored) {
            // optional — leave Environment unchanged if .env unreadable
        }
        return values;
    }
}
