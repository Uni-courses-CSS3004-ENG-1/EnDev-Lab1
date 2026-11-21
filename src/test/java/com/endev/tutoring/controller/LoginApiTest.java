package com.endev.tutoring.controller;

import com.endev.tutoring.TutoringApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The real dev users, the real BCrypt check and the real signer on the dev profile.
@SpringBootTest(classes = TutoringApplication.class)
@AutoConfigureMockMvc
class LoginApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtDecoder decoder;

    @Autowired
    private ObjectMapper json;

    @Test
    void devTutorGetsATokenTheApiAccepts() throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"username\":\"dana\",\"password\":\"tutor-dev\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.expiresIn", is(900)))
                .andReturn().getResponse().getContentAsString();

        Jwt jwt = decoder.decode(json.readTree(body).get("accessToken").asText());
        assertAll(
                () -> assertEquals("dana", jwt.getSubject()),
                () -> assertEquals("tutoring", jwt.getClaimAsString("iss")),
                () -> assertEquals(List.of("TUTOR"), jwt.getClaimAsStringList("roles")));
    }

    @Test
    void wrongPasswordAndUnknownUserGiveTheSameBody() throws Exception {
        String wrongPassword = mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
                        .header("X-Correlation-Id", "same")
                        .content("{\"username\":\"aliya\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
                        .header("X-Correlation-Id", "same")
                        .content("{\"username\":\"nobody\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json(wrongPassword, true));
    }
}
