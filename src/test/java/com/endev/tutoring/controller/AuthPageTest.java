package com.endev.tutoring.controller;

import com.endev.tutoring.TutoringApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The auth page is ours, open without a token, and opening it does not open /api.
@SpringBootTest(classes = TutoringApplication.class)
@AutoConfigureMockMvc
class AuthPageTest {

    @Autowired
    private MockMvc mvc;

    // index.html is the shell React draws into; the product, roles, dev users and the
    // password field live in app.js, built from frontend/ by `npm run build`.
    @Test
    void rootIsOurPageNotSpringsLoginForm() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
        mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Tutoring marketplace"),
                        containsString("<div id=\"root\">"),
                        containsString("src=\"/app.js\""),
                        containsString("href=\"/app.css\""),
                        not(containsString("Please sign in")))));
        mvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("STUDENT"), containsString("TUTOR"),
                        containsString("/api/v1/auth/login"), containsString("/api/v1/auth/refresh"),
                        containsString("\"password\""),
                        not(containsString("Please sign in")))));
    }

    @Test
    void scriptAndStylesNeedNoTokenAndTheScriptStoresNothing() throws Exception {
        mvc.perform(get("/app.css")).andExpect(status().isOk());
        mvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Authorization"),
                        containsString("X-Correlation-Id"),
                        not(containsString("localStorage.")),
                        not(containsString("sessionStorage.")),
                        not(containsString("document.cookie")))));
    }

    @Test
    void openingThePageDoesNotOpenTheApi() throws Exception {
        mvc.perform(get("/api/v1/lessons/7d3f0a52-1c2b-4c55-9a51-2f9d0c1e4b6a"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/other.html")).andExpect(status().isUnauthorized());
    }
}
