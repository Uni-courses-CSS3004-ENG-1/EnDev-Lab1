package com.endev.tutoring.controller;

import com.endev.tutoring.TutoringApplication;
import com.endev.tutoring.exception.LessonTransitionException;
import com.endev.tutoring.model.Lesson;
import com.endev.tutoring.model.LessonId;
import com.endev.tutoring.model.LessonStatus;
import com.endev.tutoring.security.SecurityConfig;
import com.endev.tutoring.service.LessonRegistrationService;
import com.endev.tutoring.service.LessonWorkflowService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.endev.tutoring.security.TestTokens.student;
import static com.endev.tutoring.security.TestTokens.tutor;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The README rules table as a web slice: the real SecurityConfig, the services mocked, and
// jwt() as the token. No jwt() means anonymous. The mocks prove a refused call never reaches them.
@WebMvcTest({LessonController.class, MeController.class})
@ContextConfiguration(classes = TutoringApplication.class)
@Import(SecurityConfig.class)
@ImportAutoConfiguration({EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class,
        HealthEndpointAutoConfiguration.class, ManagementContextAutoConfiguration.class})
class SecurityRulesTest {

    private static final UUID ID = UUID.fromString("7d3f0a52-1c2b-4c55-9a51-2f9d0c1e4b6a");
    private static final String CONFIRM = "/api/v1/lessons/" + ID + "/confirm";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private LessonRegistrationService registration;

    @MockitoBean
    private LessonWorkflowService workflow;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static Lesson lesson(LessonStatus status) {
        return new Lesson(ID, new LessonId("LES-2026-0042"), status, "Algebra");
    }

    @Nested
    @DisplayName("no token: 401")
    class NoToken {

        @Test
        void readingALessonIs401AndNeverReachesTheService() throws Exception {
            mvc.perform(get("/api/v1/lessons/{id}", ID).header("X-Correlation-Id", "rules-1"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("UNAUTHENTICATED")))
                    .andExpect(jsonPath("$.message", is("valid token required")))
                    .andExpect(jsonPath("$.correlationId", is("rules-1")))
                    .andExpect(jsonPath("$.*", hasSize(3)));
            verifyNoInteractions(registration);
        }

        @Test
        void creatingALessonIs401Not201() throws Exception {
            mvc.perform(post("/api/v1/lessons").contentType(APPLICATION_JSON)
                            .content("{\"businessKey\":\"LES-2026-0042\",\"subject\":\"Algebra\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("UNAUTHENTICATED")));
            verifyNoInteractions(registration);
        }

        @Test
        void confirmingIs401() throws Exception {
            mvc.perform(post(CONFIRM))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("UNAUTHENTICATED")));
            verifyNoInteractions(workflow);
        }

        @Test
        void healthStaysOpen() throws Exception {
            mvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("UP")));
        }
    }

    @Nested
    @DisplayName("GET /api/me")
    class Me {

        @Test
        void returnsTheUsernameAndAuthoritiesFromTheToken() throws Exception {
            mvc.perform(get("/api/me").with(student()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username", is("aliya")))
                    .andExpect(jsonPath("$.authorities", contains("ROLE_STUDENT")));
        }

        @Test
        void aTutorTokenIsATutor() throws Exception {
            mvc.perform(get("/api/me").with(tutor()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username", is("dana")))
                    .andExpect(jsonPath("$.authorities", contains("ROLE_TUTOR")));
        }

        @Test
        void noTokenIs401() throws Exception {
            mvc.perform(get("/api/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("UNAUTHENTICATED")));
        }
    }

    @Nested
    @DisplayName("student: 403 on confirm")
    class Student {

        @Test
        void confirmingIs403AndNeverReachesTheService() throws Exception {
            mvc.perform(post(CONFIRM).with(student()).header("X-Correlation-Id", "rules-2"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code", is("FORBIDDEN")))
                    .andExpect(jsonPath("$.message", is("not allowed for your role")))
                    .andExpect(jsonPath("$.correlationId", is("rules-2")))
                    .andExpect(jsonPath("$.*", hasSize(3)));
            verifyNoInteractions(workflow);
        }

        @Test
        void typingTheRoleIntoTheBodyChangesNothing() throws Exception {
            mvc.perform(post(CONFIRM).with(student()).contentType(APPLICATION_JSON)
                            .content("{\"role\":\"TUTOR\",\"actor\":\"dana\"}"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(workflow);
        }

        @Test
        void readingIsAllowed() throws Exception {
            when(registration.find(ID)).thenReturn(lesson(LessonStatus.REQUESTED));

            mvc.perform(get("/api/v1/lessons/{id}", ID).with(student()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("REQUESTED")));
        }
    }

    @Nested
    @DisplayName("tutor: confirm succeeds")
    class Tutor {

        @Test
        void confirmingReturnsTheNewStatusAndTheActorIsTheTokenSubject() throws Exception {
            when(workflow.confirm(ID, "dana")).thenReturn(lesson(LessonStatus.CONFIRMED));

            mvc.perform(post(CONFIRM).with(tutor()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(ID.toString())))
                    .andExpect(jsonPath("$.status", is("CONFIRMED")));
            verify(workflow).confirm(ID, "dana");
        }

        @Test
        void anActorInTheBodyIsIgnored() throws Exception {
            when(workflow.confirm(ID, "dana")).thenReturn(lesson(LessonStatus.CONFIRMED));

            mvc.perform(post(CONFIRM).with(tutor()).contentType(APPLICATION_JSON)
                            .content("{\"actor\":\"someone-else\"}"))
                    .andExpect(status().isOk());
            verify(workflow).confirm(ID, "dana");
        }

        @Test
        void aMoveTheRulesRefuseIsNotA200() throws Exception {
            when(workflow.confirm(any(), any()))
                    .thenThrow(new LessonTransitionException("Cannot move a lesson from COMPLETED to CONFIRMED"));

            mvc.perform(post(CONFIRM).with(tutor()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code", is("INVALID_TRANSITION")))
                    .andExpect(jsonPath("$.message", is("Cannot move a lesson from COMPLETED to CONFIRMED")));
        }

        @Test
        void aPathIdThatIsNotAUuidIs400() throws Exception {
            mvc.perform(post("/api/v1/lessons/LES-2026-0042/confirm").with(tutor()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", is("id must be a UUID")));
            verifyNoInteractions(workflow);
        }
    }
}
