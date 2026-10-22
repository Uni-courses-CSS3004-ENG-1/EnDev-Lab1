package com.endev.tutoring.config;

import com.endev.tutoring.domain.Rule;
import com.endev.tutoring.handler.LessonService;
import com.endev.tutoring.persistence.LessonEntity;
import jakarta.persistence.EntityManagerFactory;
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

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void contextLoads() {
        assertNotNull(rule);
        assertNotNull(lessonService);
    }

    // the context only starts when ddl-auto: validate agrees with the lesson table
    @Test
    void lessonEntityIsMapped() {
        assertNotNull(entityManagerFactory.getMetamodel().entity(LessonEntity.class));
    }
}
