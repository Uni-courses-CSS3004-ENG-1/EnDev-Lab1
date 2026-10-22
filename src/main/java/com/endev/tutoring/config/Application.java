package com.endev.tutoring.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

// Application is in the config package, so we tell Spring to scan the whole project.
// Entities are looked up from here too, so @EntityScan points at persistence.
@SpringBootApplication(scanBasePackages = "com.endev.tutoring")
@EntityScan(basePackages = "com.endev.tutoring.persistence")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
