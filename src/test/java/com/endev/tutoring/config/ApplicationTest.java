package com.endev.tutoring.config;

import com.endev.tutoring.domain.Rule;
import com.endev.tutoring.handler.LessonService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class ApplicationTest {

    @Autowired
    private Rule rule;

    @Autowired
    private LessonService service;

    @Test
    @DisplayName("the Spring context loads with the rule chain and the service")
    void contextLoads() {
        assertNotNull(rule);
        assertNotNull(service);
    }
}
