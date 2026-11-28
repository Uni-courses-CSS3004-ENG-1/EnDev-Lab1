package com.endev.tutoring.controller;

import com.endev.tutoring.TutoringApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.allOf;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Staying logged in across page loads: the httpOnly refresh cookie, and what it may not do.
@SpringBootTest(classes = TutoringApplication.class)
@AutoConfigureMockMvc
class SessionCookieTest {

    @Autowired
    private MockMvc mvc;

    private MvcResult login() throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"username\":\"aliya\",\"password\":\"student-dev\"}"))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    void loginSetsAnHttpOnlyStrictCookieOnlyTheAuthPathsSee() throws Exception {
        String setCookie = login().getResponse().getHeader("Set-Cookie");
        assertAll(
                () -> assertTrue(setCookie.startsWith("refresh_token="), setCookie),
                () -> assertTrue(setCookie.contains("HttpOnly"), setCookie),
                () -> assertTrue(setCookie.contains("SameSite=Strict"), setCookie),
                () -> assertTrue(setCookie.contains("Path=/api/v1/auth"), setCookie));
    }

    @Test
    void theCookieBuysANewAccessTokenThatWorks() throws Exception {
        Cookie refresh = login().getResponse().getCookie("refresh_token");

        String body = mvc.perform(post("/api/v1/auth/refresh").cookie(refresh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.expiresIn", is(900)))
                .andExpect(header().string("Set-Cookie", containsString("refresh_token=")))
                .andReturn().getResponse().getContentAsString();
        String access = new ObjectMapper().readTree(body).get("accessToken").asText();

        mvc.perform(get("/api/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("aliya")));
    }

    @Test
    void noCookieOrAForgedOneIs401() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHENTICATED")))
                .andExpect(jsonPath("$.message", is("session expired, log in again")));
        mvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("refresh_token", "a.b.c")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aRefreshTokenIsNotAnAccessToken() throws Exception {
        String refresh = login().getResponse().getCookie("refresh_token").getValue();

        mvc.perform(get("/api/me").header("Authorization", "Bearer " + refresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anAccessTokenCannotRefresh() throws Exception {
        String body = login().getResponse().getContentAsString();
        String access = new ObjectMapper().readTree(body).get("accessToken").asText();

        mvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("refresh_token", access)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRemovesTheCookie() throws Exception {
        mvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", allOf(
                        containsString("refresh_token=;"), containsString("Max-Age=0"), not(containsString("eyJ")))));
    }
}
