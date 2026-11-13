package com.endev.tutoring.controller;

import com.endev.tutoring.dto.ApiError;
import com.endev.tutoring.dto.CreateLessonRequest;
import com.endev.tutoring.dto.LessonResponse;
import com.endev.tutoring.service.LessonRegistrationService;

import com.endev.tutoring.model.Lesson;
import com.endev.tutoring.model.LessonId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

// HTTP adapter for lessons. The entity remains behind the registration service.
@RestController
@RequestMapping(path = "/api/v1/lessons", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Lessons", description = "Create lessons and find them by UUID")
public class LessonController {

    private final LessonRegistrationService registration;

    public LessonController(LessonRegistrationService registration) {
        this.registration = registration;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a lesson")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Lesson created",
                    headers = @Header(name = "Location", description = "URI of the created lesson",
                            schema = @Schema(type = "string", format = "uri")),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LessonResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid lesson request",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Business key already exists",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<LessonResponse> create(@Valid @RequestBody CreateLessonRequest request) {
        Lesson lesson = registration.create(new LessonId(request.businessKey()), request.subject());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(lesson.id())
                .toUri();
        return ResponseEntity.created(location).body(LessonResponse.from(lesson));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find a lesson by its UUID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lesson found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LessonResponse.class))),
            @ApiResponse(responseCode = "400", description = "Identifier is not a UUID",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Lesson not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<LessonResponse> get(@PathVariable(name = "id") UUID id) {
        return ResponseEntity.ok(LessonResponse.from(registration.find(id)));
    }
}