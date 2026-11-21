package com.endev.tutoring.controller;

import com.endev.tutoring.TutoringApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The marker's path with nothing mocked: dev users, BCrypt, real signed tokens, the chain,
// @PreAuthorize, the rules and PostgreSQL.
@SpringBootTest(classes = TutoringApplication.class)
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class AuthFlowTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper json;

    @BeforeEach
    void cleanRows() {
        jdbc.update("DELETE FROM lesson");
    }

    private String login(String username, String password) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(body).get("accessToken").asText();
    }

    private String storedStatus(UUID id) {
        return jdbc.queryForObject("SELECT status FROM lesson WHERE id = ?", String.class, id);
    }

    @Test
    void studentIsRefusedTutorConfirmsAndLoggingOutIs401(CapturedOutput output) throws Exception {
        String student = login("aliya", "student-dev");

        mvc.perform(get("/api/me").header("Authorization", student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("aliya")))
                .andExpect(jsonPath("$.authorities", contains("ROLE_STUDENT")));

        String created = mvc.perform(post("/api/v1/lessons").header("Authorization", student)
                        .contentType(APPLICATION_JSON)
                        .content("{\"businessKey\":\"LES-2026-0900\",\"subject\":\"Algebra\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("REQUESTED")))
                .andReturn().getResponse().getContentAsString();
        UUID lesson = UUID.fromString(json.readTree(created).get("id").asText());

        mvc.perform(post("/api/v1/lessons/{id}/confirm", lesson).header("Authorization", student))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("FORBIDDEN")));
        assertEquals("REQUESTED", storedStatus(lesson), "a 403 must not move the row");

        String tutor = login("dana", "tutor-dev");
        mvc.perform(post("/api/v1/lessons/{id}/confirm", lesson).header("Authorization", tutor)
                        .contentType(APPLICATION_JSON).content("{\"actor\":\"aliya\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")));
        assertEquals("CONFIRMED", storedStatus(lesson));

        mvc.perform(post("/api/v1/lessons/{id}/confirm", lesson).header("Authorization", tutor))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("INVALID_TRANSITION")));
        assertEquals("CONFIRMED", storedStatus(lesson));

        // logging out is the client dropping its token; the next call has none
        mvc.perform(get("/api/v1/lessons/{id}", lesson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHENTICATED")));

        String log = output.getOut();
        assertAll(
                () -> assertTrue(log.contains("lesson LES-2026-0900 confirmed by dana"), "the log names the actor"),
                () -> assertFalse(log.contains(tutor.substring("Bearer ".length())), "the log holds no token"),
                () -> assertFalse(log.contains("tutor-dev"), "the log holds no password"));
    }
}
