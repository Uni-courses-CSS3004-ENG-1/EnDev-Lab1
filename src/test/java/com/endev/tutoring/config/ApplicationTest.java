package com.endev.tutoring.config;

import com.endev.tutoring.domain.Rule;
import com.endev.tutoring.handler.LessonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class ApplicationTest {

    @Autowired
    private Rule rule;

    @Autowired
    private LessonService lessonService;

    @Test
    void contextLoads() {
        assertNotNull(rule);
        assertNotNull(lessonService);
    }
}
