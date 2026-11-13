package com.endev.tutoring.controller;

import com.endev.tutoring.dto.ApiError;
import com.endev.tutoring.dto.CreateTutorRequest;
import com.endev.tutoring.dto.TutorResponse;
import com.endev.tutoring.model.Tutor;
import com.endev.tutoring.service.TutorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

@RestController
@RequestMapping(path = "/api/v1/tutors", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Tutors", description = "Create tutors and find them by UUID")
public class TutorController {

    private final TutorService tutors;

    public TutorController(TutorService tutors) {
        this.tutors = tutors;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a tutor")
    @ApiResponse(responseCode = "201", description = "Tutor created")
    @ApiResponse(responseCode = "400", description = "Invalid tutor request",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Email already exists",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<TutorResponse> create(@Valid @RequestBody CreateTutorRequest request) {
        Tutor tutor = tutors.create(request.name(), request.email(), request.subject(), request.hourlyRate());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(tutor.id()).toUri();
        return ResponseEntity.created(location).body(TutorResponse.from(tutor));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find a tutor by its UUID")
    @ApiResponse(responseCode = "200", description = "Tutor found")
    @ApiResponse(responseCode = "404", description = "Tutor not found",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public TutorResponse get(@PathVariable(name = "id") UUID id) {
        return TutorResponse.from(tutors.find(id));
    }
}
