package com.endev.tutoring.config;

import com.endev.tutoring.client.DownstreamProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// Registers the outbound settings, so the client package only declares what it needs
// and config decides where it comes from.
@Configuration
@EnableConfigurationProperties(DownstreamProperties.class)
public class ClientConfig {
}
