package com.endev.tutoring.config;

import com.endev.tutoring.client.DownstreamProperties;
import com.endev.tutoring.client.LoggingPaymentClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

// Boots the whole application on the dev profile and reads the values Spring actually bound,
// not the yaml text. Nothing here opens a socket to port 9090: the payment client only holds
// the address until Lab 6 makes the call.
@SpringBootTest(classes = Application.class)
@ActiveProfiles("dev")
class DevProfileTest {

    @Autowired
    private Environment environment;

    @Autowired
    private DownstreamProperties downstream;

    @Autowired
    private LoggingPaymentClient paymentClient;

    @Nested
    @DisplayName("the dev profile")
    class Dev {

        @Test
        void isTheOnlyActiveProfile() {
            assertArrayEquals(new String[] {"dev"}, environment.getActiveProfiles());
        }

        @Test
        void bindsTheLaptopDownstream() {
            assertAll(
                    () -> assertEquals(URI.create("http://localhost:9090"), downstream.baseUrl()),
                    () -> assertEquals(Duration.ofSeconds(2), downstream.timeout()));
        }

        @Test
        void usesTheClassroomDatabaseOnLocalhost() {
            assertAll(
                    () -> assertEquals("jdbc:postgresql://localhost:5432/css",
                            environment.getProperty("spring.datasource.url")),
                    () -> assertEquals("css", environment.getProperty("spring.datasource.username")));
        }
    }

    @Nested
    @DisplayName("the payment adapter")
    class PaymentAdapter {

        // the outbound port is configured with the bound values, not a copy of them
        @Test
        void receivesTheSameDownstreamProperties() {
            assertSame(downstream, paymentClient.downstream());
        }
    }
}
