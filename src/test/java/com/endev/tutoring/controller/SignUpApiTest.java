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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// POST /api/v1/auth/register against PostgreSQL: the account and its profile row are one transaction,
// the password is stored as a BCrypt hash, and the answer is a token like login's.
@SpringBootTest(classes = TutoringApplication.class)
@AutoConfigureMockMvc
class SignUpApiTest {

    private static final String STUDENT = """
            {"username":"test-madina","password":"correct-horse","role":"STUDENT",
             "name":"Madina Sultan","email":"madina@signup.test"}""";
    private static final String TUTOR = """
            {"username":"test-ruslan","password":"correct-horse","role":"TUTOR",
             "name":"Ruslan Ospanov","email":"ruslan@signup.test",
             "subject":"Biology","hourlyRate":20.00,"bio":"Cells, genes and ecosystems."}""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper json;

    @AfterEach
    void removeTestAccounts() {
        cleanTestAccounts();
    }

    @BeforeEach
    void cleanTestAccounts() {
        jdbc.update("DELETE FROM lesson");
        jdbc.update("DELETE FROM app_user WHERE username LIKE 'test-%' OR username LIKE '%@signup.test'");
        jdbc.update("DELETE FROM student WHERE email LIKE '%@signup.test'");
        jdbc.update("DELETE FROM tutor WHERE email LIKE '%@signup.test'");
    }

    private ResultActions register(String body) throws Exception {
        return mvc.perform(post("/api/v1/auth/register").contentType(APPLICATION_JSON).content(body));
    }

    private int count(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    @Test
    void aStudentSignsUpAndIsLoggedIn() throws Exception {
        String body = register(STUDENT)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/account"))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.expiresIn", is(900)))
                .andReturn().getResponse().getContentAsString();
        String bearer = "Bearer " + json.readTree(body).get("accessToken").asText();

        mvc.perform(get("/api/me").header("Authorization", bearer))
                .andExpect(jsonPath("$.username", is("test-madina")))
                .andExpect(jsonPath("$.authorities", contains("ROLE_STUDENT")));
        mvc.perform(get("/api/v1/account").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Madina Sultan")))
                .andExpect(jsonPath("$.role", is("STUDENT")));

        String hash = jdbc.queryForObject("SELECT password_hash FROM app_user WHERE username = 'test-madina'",
                String.class);
        assertAll(
                () -> assertTrue(hash.startsWith("{bcrypt}$2"), hash),
                () -> assertFalse(hash.contains("correct-horse")),
                () -> assertFalse(body.contains("correct-horse")));
    }

    @Test
    void aTutorSignsUpAndAppearsInThePublicCatalogue() throws Exception {
        register(TUTOR).andExpect(status().isCreated());

        // no token: the catalogue is public, and it carries no email
        String catalogue = mvc.perform(get("/api/v1/tutors"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertAll(
                () -> assertTrue(catalogue.contains("Ruslan Ospanov")),
                () -> assertTrue(catalogue.contains("Cells, genes and ecosystems.")),
                () -> assertFalse(catalogue.contains("@signup.test")));
    }

    @Test
    void theNewAccountLogsInWithItsPassword() throws Exception {
        register(STUDENT).andExpect(status().isCreated());

        mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"username\":\"test-madina\",\"password\":\"correct-horse\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"username\":\"test-madina\",\"password\":\"wrong-horse\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("bad username or password")));
    }

    // the page signs people up with only an email and a password
    @Test
    void signUpWithoutAUsernameThenLogInWithTheEmail() throws Exception {
        register("""
                {"password":"correct-horse","role":"STUDENT","name":"Email Only","email":"Email.Only@Signup.Test"}""")
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"username\":\"email.only@signup.test\",\"password\":\"correct-horse\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"username\":\"EMAIL.ONLY@signup.test\",\"password\":\"wrong-horse\"}"))
                .andExpect(status().isUnauthorized());
        assertEquals(1, count("SELECT count(*) FROM app_user WHERE username = 'email.only@signup.test'"));
        jdbc.update("DELETE FROM app_user WHERE username = 'email.only@signup.test'");
    }

    @Test
    void aTakenUsernameIs409AndWritesNoProfile() throws Exception {
        register(STUDENT).andExpect(status().isCreated());

        register(STUDENT.replace("madina@signup.test", "other@signup.test"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("DUPLICATE")))
                .andExpect(jsonPath("$.message", is("username already taken: test-madina")));
        assertEquals(0, count("SELECT count(*) FROM student WHERE email = 'other@signup.test'"));
    }

    @Test
    void aDevUsernameIsTakenToo() throws Exception {
        register(STUDENT.replace("test-madina", "aliya"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("username already taken: aliya")));
    }

    @Test
    void aTakenEmailIs409AndWritesNoAccount() throws Exception {
        register(STUDENT).andExpect(status().isCreated());

        register(STUDENT.replace("test-madina", "test-madina2"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("email already exists: madina@signup.test")));
        assertEquals(0, count("SELECT count(*) FROM app_user WHERE username = 'test-madina2'"));
    }

    @Test
    void aTutorWithoutASubjectIs400AndWritesNothing() throws Exception {
        register(TUTOR.replace("\"subject\":\"Biology\",", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION")))
                .andExpect(jsonPath("$.message", is("subject is required for a tutor")));
        assertAll(
                () -> assertEquals(0, count("SELECT count(*) FROM app_user WHERE username = 'test-ruslan'")),
                () -> assertEquals(0, count("SELECT count(*) FROM tutor WHERE email = 'ruslan@signup.test'")));
    }

    @Test
    void badFieldsAre400() throws Exception {
        register(STUDENT.replace("correct-horse", "short"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("password size must be between 8 and 72")));
        register(STUDENT.replace("\"STUDENT\"", "\"ADMIN\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("role must be STUDENT or TUTOR")));
        register(STUDENT.replace("test-madina", "Test Madina"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is(
                        "username must be 3-32 characters: lowercase letters, digits, dot, dash or underscore")));
        assertEquals(0, count("SELECT count(*) FROM student WHERE email = 'madina@signup.test'"));
    }
}
