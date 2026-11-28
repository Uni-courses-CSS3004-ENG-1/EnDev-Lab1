package com.endev.tutoring.controller;

import com.endev.tutoring.TutoringApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// A student books dana, dana confirms and completes, and nobody else may touch the lesson.
// Real dev accounts, real tokens, the chain, @PreAuthorize, the rules and PostgreSQL.
@SpringBootTest(classes = TutoringApplication.class)
@AutoConfigureMockMvc
class BookingFlowTest {

    // tomorrow at a whole hour, so it is always @Future
    private static final OffsetDateTime TOMORROW = OffsetDateTime.now(ZoneOffset.UTC)
            .plusDays(1).truncatedTo(ChronoUnit.HOURS);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper json;

    private String aliya;
    private String dana;
    private String timur;
    private UUID danaTutorId;

    @AfterEach
    void removeTestLessons() {
        jdbc.update("DELETE FROM lesson");
    }

    @BeforeEach
    void cleanRowsAndLogIn() throws Exception {
        jdbc.update("DELETE FROM lesson");
        aliya = login("aliya", "student-dev");
        dana = login("dana", "tutor-dev");
        timur = login("timur", "tutor-dev");
        danaTutorId = jdbc.queryForObject("SELECT tutor_id FROM app_user WHERE username = 'dana'", UUID.class);
    }

    private String login(String username, String password) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(body).get("accessToken").asText();
    }

    private ResultActions book(String bearer, UUID tutorId, OffsetDateTime startsAt) throws Exception {
        return mvc.perform(post("/api/v1/lessons/book").header("Authorization", bearer)
                .contentType(APPLICATION_JSON)
                .content("{\"tutorId\":\"%s\",\"subject\":\"Algebra\",\"startsAt\":\"%s\"}"
                        .formatted(tutorId, startsAt)));
    }

    private UUID booked() throws Exception {
        String body = book(aliya, danaTutorId, TOMORROW).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(body).get("id").asText());
    }

    private ResultActions move(String bearer, UUID id, String verb) throws Exception {
        return mvc.perform(post("/api/v1/lessons/{id}/" + verb, id).header("Authorization", bearer));
    }

    private String storedStatus(UUID id) {
        return jdbc.queryForObject("SELECT status FROM lesson WHERE id = ?", String.class, id);
    }

    @Test
    void aStudentBooksAndTheLessonCarriesBothNames() throws Exception {
        book(aliya, danaTutorId, TOMORROW)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("REQUESTED")))
                .andExpect(jsonPath("$.businessKey", startsWith("LES-")))
                .andExpect(jsonPath("$.tutorName", is("Dana Smith")))
                .andExpect(jsonPath("$.studentName", is("Aliya Nurlanova")))
                .andExpect(jsonPath("$.hourlyRate", is(25.0)));
    }

    @Test
    void eachSideSeesItsOwnLessons() throws Exception {
        booked();

        mvc.perform(get("/api/v1/lessons/mine").header("Authorization", aliya))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/v1/lessons/mine").header("Authorization", dana))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].studentName", is("Aliya Nurlanova")));
        mvc.perform(get("/api/v1/lessons/mine").header("Authorization", timur))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/v1/lessons/mine")).andExpect(status().isUnauthorized());
    }

    @Test
    void aTutorCannotBook() throws Exception {
        book(dana, danaTutorId, TOMORROW)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("FORBIDDEN")));
    }

    @Test
    void aClashingTimeIs409AndAPastTimeIs400() throws Exception {
        booked();

        book(aliya, danaTutorId, TOMORROW.plusMinutes(30))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("SLOT_TAKEN")));
        book(aliya, danaTutorId, TOMORROW.plusHours(1)).andExpect(status().isCreated());
        book(aliya, danaTutorId, OffsetDateTime.now().minusDays(1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("startsAt must be a future date")));
        book(aliya, UUID.randomUUID(), TOMORROW.plusDays(1)).andExpect(status().isNotFound());
    }

    @Test
    void theTutorConfirmsThenCompletes() throws Exception {
        UUID lesson = booked();

        move(dana, lesson, "confirm").andExpect(status().isOk()).andExpect(jsonPath("$.status", is("CONFIRMED")));
        move(dana, lesson, "complete").andExpect(status().isOk()).andExpect(jsonPath("$.status", is("COMPLETED")));
        assertEquals("COMPLETED", storedStatus(lesson));

        // the catalogue counts it
        mvc.perform(get("/api/v1/tutors"))
                .andExpect(jsonPath("$[?(@.id == '%s')].lessonsTaught".formatted(danaTutorId), contains(1)));
    }

    @Test
    void anotherTutorMayNotTouchDanasLesson() throws Exception {
        UUID lesson = booked();

        move(timur, lesson, "confirm")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("not your lesson")));
        move(timur, lesson, "cancel").andExpect(status().isForbidden());
        assertEquals("REQUESTED", storedStatus(lesson));
    }

    @Test
    void aStudentMayNotCompleteButMayCancelTheirBooking() throws Exception {
        UUID lesson = booked();

        move(aliya, lesson, "complete")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("not allowed for your role")));
        move(aliya, lesson, "cancel").andExpect(status().isOk()).andExpect(jsonPath("$.status", is("CANCELLED")));
        move(dana, lesson, "confirm")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("INVALID_TRANSITION")));
        assertEquals("CANCELLED", storedStatus(lesson));
    }

    @Test
    void aRequestedLessonCannotBeCompleted() throws Exception {
        UUID lesson = booked();

        move(dana, lesson, "complete").andExpect(status().isConflict());
        assertEquals("REQUESTED", storedStatus(lesson));
    }
}
