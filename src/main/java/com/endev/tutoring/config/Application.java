package com.endev.tutoring.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Application is in the config package, so we tell Spring to scan the whole project
@SpringBootApplication(scanBasePackages = "com.endev.tutoring")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
