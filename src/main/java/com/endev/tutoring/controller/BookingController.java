package com.endev.tutoring.controller;

import com.endev.tutoring.dto.ApiError;
import com.endev.tutoring.dto.BookLessonRequest;
import com.endev.tutoring.dto.LessonDetailsResponse;
import com.endev.tutoring.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

// Booking and "my lessons". Who the caller is comes from Authentication.getName(), the token's subject.
@RestController
@RequestMapping(path = "/api/v1/lessons", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Booking", description = "A student books a tutor; everyone sees their own lessons")
public class BookingController {

    private final BookingService booking;

    public BookingController(BookingService booking) {
        this.booking = booking;
    }

    @PostMapping(path = "/book", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Book a one-hour lesson with a tutor (STUDENT only)")
    @ApiResponse(responseCode = "201", description = "Lesson booked; status is REQUESTED")
    @ApiResponse(responseCode = "400", description = "A field is missing, or the time is in the past",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "valid token required",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "A TUTOR cannot book",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Tutor not found",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "The tutor already has a lesson at that time",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<LessonDetailsResponse> book(@Valid @RequestBody BookLessonRequest request,
                                                      Authentication authentication) {
        var lesson = LessonDetailsResponse.from(booking.book(authentication.getName(), request.tutorId(),
                request.subject(), request.startsAt()));
        return ResponseEntity.created(URI.create("/api/v1/lessons/" + lesson.id())).body(lesson);
    }

    @GetMapping("/mine")
    @Operation(summary = "My lessons: a tutor's teaching, or a student's bookings")
    @ApiResponse(responseCode = "200", description = "Soonest first")
    @ApiResponse(responseCode = "401", description = "valid token required",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public List<LessonDetailsResponse> mine(Authentication authentication) {
        return booking.mine(authentication.getName()).stream().map(LessonDetailsResponse::from).toList();
    }
}
