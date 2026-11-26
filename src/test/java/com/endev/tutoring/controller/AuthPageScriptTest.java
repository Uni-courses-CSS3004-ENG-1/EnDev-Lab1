package com.endev.tutoring.controller;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Locks the auth page's in-memory token-drop paths without starting Spring or PostgreSQL.
class AuthPageScriptTest {

    private static final String SCRIPT = readScript();

    @Test
    void hasOneDropTokenFunctionThatClearsTheToken() {
        assertEquals(1, count("function dropToken("), "there must be one dropToken function");
        assertTrue(functionBody("dropToken(message, historyText)").contains("accessToken = null"),
                "dropToken must clear the in-memory access token");
    }

    @Test
    void logoutDelegatesToDropTokenInsteadOfClearingTheTokenAgain() {
        String logout = bodyAfter("els.logout.addEventListener('click', () =>");

        assertTrue(logout.contains("dropToken("), "the logout click handler must call dropToken");
        assertFalse(logout.contains("accessToken = null"),
                "the logout click handler must not duplicate the token clear");
        // Declaration, dropToken, pre-login cleanup, and the failed GET /api/me path.
        assertEquals(4, count("accessToken = null"),
                "a new token clear needs an intentional update to this test");
    }

    @Test
    void expiryDropsTheToken() {
        String tick = functionBody("tick()");

        assertTrue(tick.contains("left === 0"), "tick must detect that no time remains");
        assertTrue(tick.contains("dropToken("), "tick must delegate expiry to dropToken");
        assertTrue(tick.contains("Session expired after 15 minutes"),
                "tick must explain that the 15-minute session expired");
    }

    @Test
    void rejectedTokenDropsTheTokenButNotForLogin() {
        String call = functionBody("call(method, path, body)");

        assertTrue(call.contains("response.status === 401"), "call must handle a 401 response");
        assertTrue(call.contains("path !== '/api/v1/auth/login'"),
                "login's own 401 must not use the authenticated-token path");
        assertTrue(call.contains("dropToken("), "an authenticated 401 must drop the token");
    }

    @Test
    void tokenIsNeverStoredOutsideTheClosure() {
        assertFalse(SCRIPT.contains("localStorage."), "the script must not access localStorage");
        assertFalse(SCRIPT.contains("sessionStorage."), "the script must not access sessionStorage");
        assertFalse(SCRIPT.contains("document.cookie"), "the script must not write a cookie");
        assertFalse(SCRIPT.contains("location.search"), "the script must not put the token in the URL query");
        assertFalse(SCRIPT.contains("location.hash"), "the script must not put the token in the URL fragment");
    }

    private static String readScript() {
        try {
            return Files.readString(Path.of("src/main/resources/static/app.js"));
        } catch (IOException exception) {
            throw new UncheckedIOException("could not read the auth page script", exception);
        }
    }

    private static int count(String text) {
        return SCRIPT.split(java.util.regex.Pattern.quote(text), -1).length - 1;
    }

    private static String functionBody(String signature) {
        return bodyAfter("function " + signature);
    }

    private static String bodyAfter(String marker) {
        int markerIndex = SCRIPT.indexOf(marker);
        assertTrue(markerIndex >= 0, "script must contain " + marker);
        int openingBrace = SCRIPT.indexOf('{', markerIndex);
        assertTrue(openingBrace >= 0, marker + " must have an opening brace");

        int depth = 0;
        for (int index = openingBrace; index < SCRIPT.length(); index++) {
            char character = SCRIPT.charAt(index);
            if (character == '{') depth++;
            if (character == '}') depth--;
            if (depth == 0) return SCRIPT.substring(openingBrace + 1, index);
        }
        throw new AssertionError(marker + " must have a closing brace");
    }
}
