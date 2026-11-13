package com.endev.tutoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Lives in the root package, so Spring finds every component and entity below it.
@SpringBootApplication
public class TutoringApplication {

    public static void main(String[] args) {
        SpringApplication.run(TutoringApplication.class, args);
    }
}
