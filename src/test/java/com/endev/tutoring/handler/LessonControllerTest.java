package com.endev.tutoring.handler;

import com.endev.tutoring.config.Application;
import com.endev.tutoring.domain.DuplicateLesson;
import com.endev.tutoring.domain.Lesson;
import com.endev.tutoring.domain.LessonId;
import com.endev.tutoring.domain.LessonNotFound;
import com.endev.tutoring.domain.LessonStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Web slice: only MVC; the service is mocked, so no PostgreSQL is involved.
@WebMvcTest(LessonController.class)
@ContextConfiguration(classes = Application.class)
class LessonControllerTest {

    private static final UUID ID = UUID.fromString("7d3f0a52-1c2b-4c55-9a51-2f9d0c1e4b6a");
    private static final String KEY = "LES-2026-0042";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private LessonRegistration registration;

    @BeforeEach
    void stubs() {
        when(registration.create(eq(new LessonId(KEY)), eq("Algebra")))
                .thenReturn(new Lesson(ID, new LessonId(KEY), LessonStatus.REQUESTED, "Algebra"));
    }

    @Nested
    @DisplayName("valid requests")
    class ValidRequests {

        @Test
        void createsWithLocation() throws Exception {
            mvc.perform(post("/api/v1/lessons")
                            .contentType(APPLICATION_JSON)
                            .content("{\"businessKey\":\"" + KEY + "\",\"subject\":\"Algebra\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/api/v1/lessons/")))
                    .andExpect(jsonPath("$.businessKey", is(KEY)));
        }

        @Test
        void acceptsTheMaximumSubjectLength() throws Exception {
            String subject = "a".repeat(120);
            when(registration.create(new LessonId(KEY), subject))
                    .thenReturn(new Lesson(ID, new LessonId(KEY), LessonStatus.REQUESTED, subject));

            mvc.perform(post("/api/v1/lessons")
                            .contentType(APPLICATION_JSON)
                            .content("{\"businessKey\":\"" + KEY + "\",\"subject\":\"" + subject + "\"}"))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("errors")
    class Errors {

        @Test
        void blankSubjectIsValidationError() throws Exception {
            postJson("{\"businessKey\":\"" + KEY + "\",\"subject\":\" \"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("VALIDATION")))
                    .andExpect(jsonPath("$.message", containsString("subject")));
            verify(registration, never()).create(any(), any());
        }

        @Test
        void blankBusinessKeyIsValidationError() throws Exception {
            postJson("{\"businessKey\":\"\",\"subject\":\"Algebra\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("VALIDATION")))
                    .andExpect(jsonPath("$.message", containsString("businessKey")));
            verify(registration, never()).create(any(), any());
        }

        @Test
        void subjectLongerThan120IsRejected() throws Exception {
            postJson("{\"businessKey\":\"" + KEY + "\",\"subject\":\"" + "a".repeat(121) + "\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("subject")));
            verify(registration, never()).create(any(), any());
        }

        @Test
        void lessonNotFoundIsNotFound() throws Exception {
            when(registration.find(ID)).thenThrow(new LessonNotFound(ID));

            mvc.perform(get("/api/v1/lessons/{id}", ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("NOT_FOUND")))
                    .andExpect(jsonPath("$.message", is("lesson not found: " + ID)));
        }

        @Test
        void duplicateIsConflict() throws Exception {
            when(registration.create(new LessonId(KEY), "Algebra"))
                    .thenThrow(new DuplicateLesson(KEY));

            postJson("{\"businessKey\":\"" + KEY + "\",\"subject\":\"Algebra\"}")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code", is("DUPLICATE")))
                    .andExpect(jsonPath("$.message", is("duplicate lesson: " + KEY)));
        }

        @Test
        void nonUuidPathIsValidationError() throws Exception {
            mvc.perform(get("/api/v1/lessons/LES-2026-0042"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", is("id must be a UUID")));
            verify(registration, never()).find(any());
        }

        @Test
        void nonJsonBodyIsValidationError() throws Exception {
            mvc.perform(post("/api/v1/lessons").contentType(APPLICATION_JSON).content("not json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", is("body must be JSON")));
        }

        @Test
        void textBodyIsUnsupported() throws Exception {
            mvc.perform(post("/api/v1/lessons").contentType(TEXT_PLAIN).content("Algebra"))
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(jsonPath("$.code", is("UNSUPPORTED")))
                    .andExpect(jsonPath("$.message", is("content type must be application/json")));
        }

        @Test
        void unexpectedDetailIsNotLeaked() throws Exception {
            when(registration.create(new LessonId(KEY), "Algebra"))
                    .thenThrow(new RuntimeException("secret detail"));

            postJson("{\"businessKey\":\"" + KEY + "\",\"subject\":\"Algebra\"}")
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code", is("INTERNAL")))
                    .andExpect(jsonPath("$.message", is("unexpected error")))
                    .andExpect(content().string(org.hamcrest.Matchers.not(containsString("secret detail"))))
                    .andExpect(content().string(org.hamcrest.Matchers.not(containsString("RuntimeException"))));
        }

        @Test
        void correlationIdIsReturnedAndBodyHasThreeFields() throws Exception {
            String correlationId = "request-42";

            mvc.perform(post("/api/v1/lessons")
                            .contentType(APPLICATION_JSON)
                            .header("X-Correlation-Id", correlationId)
                            .content("{\"businessKey\":\"\",\"subject\":\"Algebra\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.correlationId", is(correlationId)))
                    .andExpect(jsonPath("$.*", hasSize(3)));
        }

        @Test
        void missingCorrelationIdGetsUuid() throws Exception {
            postJson("{\"businessKey\":\"\",\"subject\":\"Algebra\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.correlationId", matchesPattern(
                            "[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")));
        }

        private org.springframework.test.web.servlet.ResultActions postJson(String body) throws Exception {
            return mvc.perform(post("/api/v1/lessons").contentType(APPLICATION_JSON).content(body));
        }
    }
}
