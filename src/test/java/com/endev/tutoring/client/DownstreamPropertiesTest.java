package com.endev.tutoring.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

// Binds app.downstream the way Spring does on start-up, without a database or a socket.
class DownstreamPropertiesTest {

    @Configuration
    @EnableConfigurationProperties(DownstreamProperties.class)
    static class Binding {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Binding.class);

    @Nested
    @DisplayName("a complete app.downstream")
    class Complete {

        @Test
        void bindsTheBaseUrlAndTheTimeoutAsADuration() {
            runner.withPropertyValues(
                            "app.downstream.base-url=http://payments.test:9090",
                            "app.downstream.timeout=2s")
                    .run(context -> {
                        DownstreamProperties downstream = context.getBean(DownstreamProperties.class);
                        assertAll(
                                () -> assertEquals(URI.create("http://payments.test:9090"), downstream.baseUrl()),
                                () -> assertEquals(Duration.ofSeconds(2), downstream.timeout()));
                    });
        }

        @Test
        void readsAnyDurationUnitNotOnlySeconds() {
            runner.withPropertyValues(
                            "app.downstream.base-url=http://payments.test:9090",
                            "app.downstream.timeout=750ms")
                    .run(context -> assertEquals(Duration.ofMillis(750),
                            context.getBean(DownstreamProperties.class).timeout()));
        }
    }

    @Nested
    @DisplayName("a missing value stops the start-up")
    class Missing {

        @Test
        void withoutABaseUrl() {
            runner.withPropertyValues("app.downstream.timeout=2s")
                    .run(context -> assertThat(context).hasFailed());
        }

        @Test
        void withoutATimeout() {
            runner.withPropertyValues("app.downstream.base-url=http://payments.test:9090")
                    .run(context -> assertThat(context).hasFailed());
        }
    }
}
