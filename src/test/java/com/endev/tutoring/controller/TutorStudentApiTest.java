package com.endev.tutoring.controller;

import com.endev.tutoring.TutoringApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Tutors, students and the lesson links against the real PostgreSQL database.
@SpringBootTest(classes = TutoringApplication.class)
@AutoConfigureMockMvc
class TutorStudentApiTest {

    private static final String TUTOR = """
            {"name":"Dana Smith","email":"dana@example.com","subject":"Algebra","hourlyRate":25.00}""";
    private static final String STUDENT = """
            {"name":"Aliya Nur","email":"aliya@example.com"}""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper json;

    @BeforeEach
    void cleanRows() {
        // lessons point at tutors and students, so they go first
        jdbc.update("DELETE FROM lesson");
        jdbc.update("DELETE FROM student");
        jdbc.update("DELETE FROM tutor");
    }

    private ResultActions postJson(String path, String body) throws Exception {
        return mvc.perform(post(path).contentType(APPLICATION_JSON).content(body));
    }

    private UUID createdId(String path, String body) throws Exception {
        String response = postJson(path, body).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(response).get("id").asText());
    }

    @Nested
    @DisplayName("tutors")
    class Tutors {

        @Test
        void createThenReadByUuid() throws Exception {
            UUID id = createdId("/api/v1/tutors", TUTOR);

            mvc.perform(get("/api/v1/tutors/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is("Dana Smith")))
                    .andExpect(jsonPath("$.hourlyRate", is(25.0)));
        }

        @Test
        void createAnswersWithLocation() throws Exception {
            postJson("/api/v1/tutors", TUTOR)
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", endsWith(
                            "/api/v1/tutors/" + jdbc.queryForObject("SELECT id FROM tutor", UUID.class))));
        }

        @Test
        void sameEmailTwiceIsConflictAndKeepsOneRow() throws Exception {
            postJson("/api/v1/tutors", TUTOR).andExpect(status().isCreated());

            postJson("/api/v1/tutors", TUTOR)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code", is("DUPLICATE")))
                    .andExpect(jsonPath("$.message", is("email already exists: dana@example.com")));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM tutor", Integer.class));
        }

        @Test
        void badEmailAndNonPositiveRateAreValidationErrors() throws Exception {
            postJson("/api/v1/tutors", """
                    {"name":"Dana","email":"not-an-email","subject":"Algebra","hourlyRate":25}""")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", is("email must be a well-formed email address")));
            postJson("/api/v1/tutors", """
                    {"name":"Dana","email":"dana@example.com","subject":"Algebra","hourlyRate":0}""")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", is("hourlyRate must be greater than 0")));
        }

        @Test
        void unknownUuidIsNotFound() throws Exception {
            UUID missing = UUID.randomUUID();
            mvc.perform(get("/api/v1/tutors/{id}", missing))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", is("tutor not found: " + missing)));
        }
    }

    @Nested
    @DisplayName("students")
    class Students {

        @Test
        void createThenReadByUuid() throws Exception {
            UUID id = createdId("/api/v1/students", STUDENT);

            mvc.perform(get("/api/v1/students/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email", is("aliya@example.com")));
        }

        @Test
        void sameEmailTwiceIsConflict() throws Exception {
            postJson("/api/v1/students", STUDENT).andExpect(status().isCreated());
            postJson("/api/v1/students", STUDENT).andExpect(status().isConflict());
        }

        @Test
        void unknownUuidIsNotFound() throws Exception {
            mvc.perform(get("/api/v1/students/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("NOT_FOUND")));
        }
    }

    @Nested
    @DisplayName("lessons linked to a tutor and a student")
    class Links {

        @Test
        void lessonKeepsBothIdsAndTheTutorName() throws Exception {
            UUID tutor = createdId("/api/v1/tutors", TUTOR);
            UUID student = createdId("/api/v1/students", STUDENT);

            UUID lesson = createdId("/api/v1/lessons", """
                    {"businessKey":"LES-2026-0500","subject":"Algebra","tutorId":"%s","studentId":"%s"}"""
                    .formatted(tutor, student));

            mvc.perform(get("/api/v1/lessons/{id}", lesson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tutorId", is(tutor.toString())))
                    .andExpect(jsonPath("$.studentId", is(student.toString())));
            assertEquals("Dana Smith", jdbc.queryForObject(
                    "SELECT tutor FROM lesson WHERE id = ?", String.class, lesson));
        }

        @Test
        void unknownTutorIsNotFoundAndWritesNothing() throws Exception {
            UUID missing = UUID.randomUUID();

            postJson("/api/v1/lessons", """
                    {"businessKey":"LES-2026-0501","subject":"Algebra","tutorId":"%s"}""".formatted(missing))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", is("tutor not found: " + missing)));
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM lesson", Integer.class));
        }

        @Test
        void unknownStudentIsNotFound() throws Exception {
            postJson("/api/v1/lessons", """
                    {"businessKey":"LES-2026-0502","subject":"Algebra","studentId":"%s"}"""
                    .formatted(UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("NOT_FOUND")));
        }

        @Test
        void lessonWithoutLinksStillWorks() throws Exception {
            UUID lesson = createdId("/api/v1/lessons", """
                    {"businessKey":"LES-2026-0503","subject":"Algebra"}""");

            mvc.perform(get("/api/v1/lessons/{id}", lesson))
                    .andExpect(jsonPath("$.tutorId").doesNotExist())
                    .andExpect(jsonPath("$.studentId").doesNotExist());
        }
    }
}
