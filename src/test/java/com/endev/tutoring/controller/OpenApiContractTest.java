package com.endev.tutoring.controller;

import com.endev.tutoring.dto.CreateLessonRequest;
import com.endev.tutoring.dto.LessonResponse;

import com.endev.tutoring.TutoringApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TutoringApplication.class)
@AutoConfigureMockMvc
class OpenApiContractTest {

    @Autowired
    private MockMvc mvc;

    @Nested
    @DisplayName("OpenAPI contract")
    class Contract {

        @Test
        void documentsLessonResponsesAndSchemas() throws Exception {
            mvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paths['/api/v1/lessons'].post.responses['201']").exists())
                    .andExpect(jsonPath("$.paths['/api/v1/lessons'].post.responses['400']").exists())
                    .andExpect(jsonPath("$.paths['/api/v1/lessons'].post.responses['409']").exists())
                    .andExpect(jsonPath("$.paths['/api/v1/lessons/{id}'].get.responses['200']").exists())
                    .andExpect(jsonPath("$.paths['/api/v1/lessons/{id}'].get.responses['404']").exists())
                    .andExpect(jsonPath("$.paths['/api/v1/lessons/{id}'].get.responses['409']").doesNotExist())
                    .andExpect(jsonPath("$.components.schemas.LessonResponse.properties.id").exists())
                    .andExpect(jsonPath("$.components.schemas.LessonResponse.properties.businessKey").exists())
                    .andExpect(jsonPath("$.components.schemas.LessonResponse.properties.status").exists())
                    .andExpect(jsonPath("$.components.schemas.LessonResponse.properties.subject").exists())
                    .andExpect(jsonPath("$.components.schemas.CreateLessonRequest.required",
                            hasItems("businessKey", "subject")))
                    .andExpect(jsonPath("$.components.schemas.LessonEntity").doesNotExist());
        }

        @Test
        void exposesSwaggerUiEntryPoint() throws Exception {
            MvcResult result = mvc.perform(get("/swagger-ui.html")).andReturn();

            int status = result.getResponse().getStatus();
            org.junit.jupiter.api.Assertions.assertTrue(status == 200 || (status >= 300 && status < 400),
                    "Swagger UI should load or redirect");
        }
    }
}