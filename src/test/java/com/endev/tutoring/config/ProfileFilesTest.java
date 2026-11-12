package com.endev.tutoring.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Reads the yaml files as written, so a password or URL cannot drift into the wrong one.
class ProfileFilesTest {

    private static PropertySource<?> yaml(String file) throws IOException {
        List<PropertySource<?>> documents = new YamlPropertySourceLoader()
                .load(file, new ClassPathResource(file));
        assertEquals(1, documents.size(), file + " should be one yaml document");
        return documents.get(0);
    }

    private static String text(PropertySource<?> source, String key) {
        Object value = source.getProperty(key);
        return value == null ? null : value.toString();
    }

    @Nested
    @DisplayName("application.yaml")
    class Shared {

        @Test
        void holdsNoConnectionDetailsAndDefaultsToDev() throws IOException {
            PropertySource<?> shared = yaml("application.yaml");
            assertAll(
                    () -> assertEquals("dev", text(shared, "spring.profiles.default")),
                    () -> assertNull(shared.getProperty("spring.profiles.active"), "never commit an active profile"),
                    () -> assertNull(shared.getProperty("spring.datasource.url")),
                    () -> assertNull(shared.getProperty("spring.datasource.password")),
                    () -> assertNull(shared.getProperty("app.downstream.base-url")),
                    () -> assertEquals("validate", text(shared, "spring.jpa.hibernate.ddl-auto")),
                    () -> assertEquals("false", text(shared, "spring.jpa.open-in-view")));
        }
    }

    @Nested
    @DisplayName("application-dev.yaml")
    class Dev {

        @Test
        void isTheLaptop() throws IOException {
            PropertySource<?> dev = yaml("application-dev.yaml");
            assertAll(
                    () -> assertEquals("jdbc:postgresql://localhost:5432/css", text(dev, "spring.datasource.url")),
                    () -> assertEquals("css", text(dev, "spring.datasource.username")),
                    () -> assertEquals("css", text(dev, "spring.datasource.password")),
                    () -> assertEquals("http://localhost:9090", text(dev, "app.downstream.base-url")),
                    () -> assertEquals("2s", text(dev, "app.downstream.timeout")));
        }
    }

    @Nested
    @DisplayName("application-prod.yaml")
    class Prod {

        @Test
        void readsTheEnvironmentWithNoDefaultSecret() throws IOException {
            PropertySource<?> prod = yaml("application-prod.yaml");
            assertAll(
                    () -> assertEquals("${CSS_DB_URL}", text(prod, "spring.datasource.url")),
                    () -> assertEquals("${CSS_DB_USER}", text(prod, "spring.datasource.username")),
                    () -> assertEquals("${CSS_DB_PASSWORD}", text(prod, "spring.datasource.password")),
                    () -> assertEquals("${CSS_DOWNSTREAM_URL}", text(prod, "app.downstream.base-url")));
        }
    }

    @Nested
    @DisplayName("src/main/java")
    class JavaSources {

        // a quoted string that starts like a connection address
        private static final Pattern URL_LITERAL = Pattern.compile("\"(jdbc:|https?://)");

        @Test
        void hasNoUrlStringLiteral() throws IOException {
            List<Path> offenders;
            try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
                offenders = files.filter(path -> path.toString().endsWith(".java"))
                        .filter(ProfileFilesTest::containsUrlLiteral)
                        .toList();
            }
            assertTrue(offenders.isEmpty(), "URL literals belong in yaml: " + offenders);
        }
    }

    private static boolean containsUrlLiteral(Path file) {
        try {
            return JavaSources.URL_LITERAL.matcher(Files.readString(file)).find();
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + file, e);
        }
    }
}
