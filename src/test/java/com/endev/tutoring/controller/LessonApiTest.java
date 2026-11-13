package com.endev.tutoring.controller;

import com.endev.tutoring.TutoringApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TutoringApplication.class)
@AutoConfigureMockMvc
class LessonApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanRows() {
        jdbc.update("DELETE FROM lesson");
    }

    @Nested
    @DisplayName("REST requests")
    class RestRequests {

        @Test
        void duplicatePostKeepsOneRowAndLocationReturnsIt() throws Exception {
            String body = body("LES-API-001", "Algebra");

            MvcResult first = mvc.perform(post("/api/v1/lessons")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andReturn();
            String location = first.getResponse().getHeader("Location");
            String firstBody = first.getResponse().getContentAsString();

            mvc.perform(post("/api/v1/lessons")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code", is("DUPLICATE")));

            assertEquals(1, count("LES-API-001"));

            mvc.perform(get(location))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.businessKey", is("LES-API-001")))
                    .andExpect(jsonPath("$.subject", is("Algebra")))
                    .andExpect(content().string(firstBody));

            UUID missing = UUID.randomUUID();
            mvc.perform(get("/api/v1/lessons/{id}", missing))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("NOT_FOUND")));

            mvc.perform(post("/api/v1/lessons")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("LES-API-002", "Geometry")))
                    .andExpect(status().isCreated());

            assertEquals(1, count("LES-API-001"));
            assertEquals(1, count("LES-API-002"));
        }
    }

    private String body(String businessKey, String subject) {
        return "{\"businessKey\":\"" + businessKey + "\",\"subject\":\"" + subject + "\"}";
    }

    private int count(String businessKey) {
        return jdbc.queryForObject("SELECT count(*) FROM lesson WHERE business_key = ?",
                Integer.class, businessKey);
    }
}