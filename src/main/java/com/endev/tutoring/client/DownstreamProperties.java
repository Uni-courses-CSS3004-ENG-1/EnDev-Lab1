package com.endev.tutoring.client;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

// Where the payment provider lives and how long a call may take, bound from app.downstream
// in the active profile's yaml. The timeout is a Duration ("2s"), never an int times 1000.
// Nothing calls the network yet: Lab 6 makes the call and enforces the timeout.
@Validated
@ConfigurationProperties(prefix = "app.downstream")
public record DownstreamProperties(@NotNull URI baseUrl, @NotNull Duration timeout) {
}
