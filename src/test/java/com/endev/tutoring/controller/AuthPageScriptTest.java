package com.endev.tutoring.controller;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Locks the page's in-memory token paths without starting Spring, PostgreSQL or a browser.
// The page is React; the token lives in one module, frontend/src/lib/api.ts, and this reads its source.
class AuthPageScriptTest {

    private static final Path FRONTEND = Path.of("frontend/src");
    private static final String SCRIPT = read(FRONTEND.resolve("lib/api.ts"));

    @Test
    void hasOneDropTokenFunctionThatClearsTheToken() {
        assertEquals(1, count("function dropToken("), "there must be one dropToken function");
        assertTrue(functionBody("dropToken(").contains("accessToken = null"),
                "dropToken must clear the in-memory access token");
    }

    @Test
    void logoutDelegatesToDropTokenInsteadOfClearingTheTokenAgain() {
        String logout = functionBody("logout(");

        assertTrue(logout.contains("/api/v1/auth/logout"), "logout must ask the server to remove the refresh cookie");
        assertTrue(logout.contains("dropToken("), "logout must call dropToken");
        assertFalse(logout.contains("accessToken = null"), "logout must not duplicate the token clear");
        // dropToken, and the clear before a new login or sign-up.
        assertEquals(2, count("accessToken = null"), "a new token clear needs an intentional update to this test");
    }

    @Test
    void theTokenIsRenewedBeforeItExpiresOrDropped() {
        String useToken = functionBody("useToken(");

        assertTrue(useToken.contains("refreshAccessToken()"), "the token must be renewed before it runs out");
        assertTrue(useToken.contains("dropToken("), "a failed renewal must drop the token");
    }

    @Test
    void rejectedTokenTriesTheCookieOnceThenDrops() {
        String call = functionBody("call<T>(");

        assertTrue(call.contains("response.status === 401"), "call must handle a 401 response");
        assertTrue(call.contains("!path.startsWith(AUTH_PATHS)"), "the auth paths' own 401 is not a rejected token");
        assertTrue(call.contains("retry && await refreshAccessToken()"), "one retry after a refresh");
        assertTrue(call.contains("dropToken("), "a rejected session must drop the token");
        assertTrue(call.contains("'X-Correlation-Id'"), "every call sends a correlation id");
    }

    // every source file of the page, not only api.ts
    @Test
    void tokenIsNeverStoredOutsideTheModule() throws IOException {
        try (Stream<Path> files = Files.walk(FRONTEND)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                String source = read(file);
                assertFalse(source.contains("localStorage."), file + " must not access localStorage");
                assertFalse(source.contains("sessionStorage."), file + " must not access sessionStorage");
                assertFalse(source.contains("document.cookie"), file + " must not write a cookie");
                assertFalse(source.contains("location.search"), file + " must not put the token in the URL query");
                assertFalse(source.contains("location.hash"), file + " must not put the token in the URL fragment");
            }
        }
        assertEquals(1, countIn(SCRIPT, "let accessToken"), "the token has one home");
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException exception) {
            throw new UncheckedIOException("could not read " + file, exception);
        }
    }

    private static int count(String text) {
        return countIn(SCRIPT, text);
    }

    private static int countIn(String source, String text) {
        return source.split(java.util.regex.Pattern.quote(text), -1).length - 1;
    }

    private static String functionBody(String signature) {
        String marker = "function " + signature;
        int markerIndex = SCRIPT.indexOf(marker);
        assertTrue(markerIndex >= 0, "script must contain " + marker);
        // the body starts at the first "{" after the closing ")" of the parameter list
        int openingBrace = SCRIPT.indexOf('{', SCRIPT.indexOf(')', markerIndex));
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
