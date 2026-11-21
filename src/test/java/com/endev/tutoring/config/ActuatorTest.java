package com.endev.tutoring.config;

import com.endev.tutoring.TutoringApplication;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static com.endev.tutoring.security.TestTokens.tutor;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TutoringApplication.class)
@AutoConfigureMockMvc
class ActuatorTest {

    @Autowired
    private MockMvc mvc;

    @Nested
    @DisplayName("exposed")
    class Exposed {

        @Test
        void healthIsUpWithoutDatasourceDetails() throws Exception {
            mvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("UP")))
                    .andExpect(jsonPath("$.components").doesNotExist())
                    .andExpect(jsonPath("$.details").doesNotExist())
                    .andExpect(content().string(not(containsString("jdbc"))))
                    .andExpect(content().string(not(containsString("postgresql"))));
        }

        @Test
        void infoContainsProductName() throws Exception {
            mvc.perform(get("/actuator/info"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.app.name", is("Tutoring marketplace")));
        }
    }

    // Not in the rules table, so denyAll: 401 without a token, 403 with one. Never the payload.
    @Nested
    @DisplayName("not exposed")
    class NotExposed {

        @ParameterizedTest
        @ValueSource(strings = {"env", "heapdump", "beans", "shutdown"})
        void managementEndpointsNeedATokenFirst(String endpoint) throws Exception {
            mvc.perform(get("/actuator/" + endpoint))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string(not(containsString("spring.datasource"))))
                    .andExpect(content().string(not(containsString("css"))));
        }

        @ParameterizedTest
        @ValueSource(strings = {"env", "heapdump", "beans", "shutdown"})
        void managementEndpointsAreForbiddenEvenForATutor(String endpoint) throws Exception {
            mvc.perform(get("/actuator/" + endpoint).with(tutor()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code", is("FORBIDDEN")))
                    .andExpect(content().string(not(containsString("spring.datasource"))));
        }
    }
}