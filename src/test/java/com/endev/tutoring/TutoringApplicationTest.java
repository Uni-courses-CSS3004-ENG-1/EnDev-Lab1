package com.endev.tutoring;

import com.endev.tutoring.rule.Rule;
import com.endev.tutoring.service.LessonService;
import com.endev.tutoring.entity.LessonEntity;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class TutoringApplicationTest {

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
