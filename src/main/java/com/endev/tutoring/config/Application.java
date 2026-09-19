package com.endev.tutoring.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// scanBasePackages is required: the class sits in config, so the default scan would
// cover config alone and never reach the services in the rings beside it.
@SpringBootApplication(scanBasePackages = "com.endev.tutoring")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
