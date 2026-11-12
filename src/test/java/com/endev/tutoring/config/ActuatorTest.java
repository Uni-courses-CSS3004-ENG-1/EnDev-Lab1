package com.endev.tutoring.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Application.class)
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

    @Nested
    @DisplayName("not exposed")
    class NotExposed {

        @ParameterizedTest
        @ValueSource(strings = {"env", "heapdump", "beans", "shutdown"})
        void managementEndpointsAreNotAvailable(String endpoint) throws Exception {
            mvc.perform(get("/actuator/" + endpoint))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string(not(containsString("spring.datasource"))))
                    .andExpect(content().string(not(containsString("css"))));
        }
    }
}