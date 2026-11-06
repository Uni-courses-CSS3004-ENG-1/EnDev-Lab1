package com.endev.tutoring.handler;

import com.endev.tutoring.config.Application;
import com.endev.tutoring.domain.Lesson;
import com.endev.tutoring.domain.LessonId;
import com.endev.tutoring.domain.LessonStatus;
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

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Web slice: only the controller and MVC. The service is a mock, so no PostgreSQL and no real server.
@WebMvcTest(LessonController.class)
@ContextConfiguration(classes = Application.class)
class LessonControllerTest {

    private static final UUID ID = UUID.fromString("7d3f0a52-1c2b-4c55-9a51-2f9d0c1e4b6a");
    private static final LessonId KEY = new LessonId("LES-2026-0042");
    private static final Lesson ALGEBRA = new Lesson(ID, KEY, LessonStatus.REQUESTED, "Algebra");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private LessonRegistration registration;

    @Nested
    @DisplayName("POST /api/v1/lessons")
    class Create {

        @Test
        void validBodyIsCreatedWithLocation() throws Exception {
            when(registration.create(KEY, "Algebra")).thenReturn(ALGEBRA);

            mvc.perform(post("/api/v1/lessons")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"businessKey": "LES-2026-0042", "subject": "Algebra"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", endsWith("/api/v1/lessons/" + ID)))
                    .andExpect(jsonPath("$.id").value(ID.toString()))
                    .andExpect(jsonPath("$.businessKey").value("LES-2026-0042"))
                    .andExpect(jsonPath("$.status").value("REQUESTED"))
                    .andExpect(jsonPath("$.subject").value("Algebra"));
        }

        // only id, businessKey, status and subject: no tutor, no created_at
        @Test
        void responseHasNoPersistenceFields() throws Exception {
            when(registration.create(KEY, "Algebra")).thenReturn(ALGEBRA);

            mvc.perform(post("/api/v1/lessons")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"businessKey": "LES-2026-0042", "subject": "Algebra"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tutor").doesNotExist())
                    .andExpect(jsonPath("$.createdAt").doesNotExist());
        }

        @Test
        void blankSubjectNeverReachesTheService() throws Exception {
            mvc.perform(post("/api/v1/lessons")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"businessKey": "LES-2026-0042", "subject": "   "}
                                    """))
                    .andExpect(status().isBadRequest());

            verify(registration, never()).create(any(), anyString());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/lessons/{id}")
    class Read {

        @Test
        void knownUuidIsReturned() throws Exception {
            when(registration.find(ID)).thenReturn(ALGEBRA);

            mvc.perform(get("/api/v1/lessons/{id}", ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ID.toString()))
                    .andExpect(jsonPath("$.businessKey").value("LES-2026-0042"))
                    .andExpect(jsonPath("$.status").value("REQUESTED"))
                    .andExpect(jsonPath("$.subject").value("Algebra"));
        }
    }
}
