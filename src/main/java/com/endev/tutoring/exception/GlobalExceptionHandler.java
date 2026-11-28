package com.endev.tutoring.exception;

import com.endev.tutoring.dto.ApiError;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Comparator;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField)
                        .thenComparing(FieldError::getDefaultMessage, Comparator.nullsFirst(String::compareTo)))
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("request is invalid");
        return error(HttpStatus.BAD_REQUEST, "VALIDATION", message, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> typeMismatch(MethodArgumentTypeMismatchException exception,
                                           HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION", exception.getName() + " must be a UUID", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadable(HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION", "body must be JSON", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiError> unsupportedMediaType(HttpServletRequest request) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED",
                "content type must be application/json", request);
    }

    @ExceptionHandler({LessonNotFoundException.class, TutorNotFoundException.class,
            StudentNotFoundException.class, AccountNotFoundException.class})
    ResponseEntity<ApiError> notFound(RuntimeException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler({DuplicateLessonException.class, DuplicateEmailException.class,
            DuplicateUsernameException.class})
    ResponseEntity<ApiError> duplicate(RuntimeException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "DUPLICATE", exception.getMessage(), request);
    }

    // the lesson rules refused the status change; the row did not move
    @ExceptionHandler(LessonTransitionException.class)
    ResponseEntity<ApiError> refusedMove(LessonTransitionException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "INVALID_TRANSITION", exception.getMessage(), request);
    }

    // a tutor sign-up without a subject or a price
    @ExceptionHandler(InvalidSignUpException.class)
    ResponseEntity<ApiError> badSignUp(InvalidSignUpException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION", exception.getMessage(), request);
    }

    // the tutor already teaches within an hour of that start time
    @ExceptionHandler(SlotTakenException.class)
    ResponseEntity<ApiError> slotTaken(SlotTakenException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "SLOT_TAKEN", exception.getMessage(), request);
    }

    // the right role, but another tutor's (or student's) lesson
    @ExceptionHandler(NotYourLessonException.class)
    ResponseEntity<ApiError> notYours(NotYourLessonException exception, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", exception.getMessage(), request);
    }

    // @PreAuthorize said no inside the controller call; the filters answer the URL rule the same way
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> wrongRole(HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "not allowed for your role", request);
    }

    // POST /api/v1/auth/refresh without a valid cookie
    @ExceptionHandler(SessionExpiredException.class)
    ResponseEntity<ApiError> sessionExpired(SessionExpiredException exception, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", exception.getMessage(), request);
    }

    // login only: unknown user and wrong password are the same answer
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> badLogin(HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "bad username or password", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> noResource(HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "no such path", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> methodNotAllowed(HttpServletRequest request) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "method not allowed", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception exception, HttpServletRequest request) {
        String correlationId = correlationId(request);
        log.error("Unexpected error correlationId={}", correlationId, exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR,
                new ApiError("INTERNAL", "unexpected error", correlationId));
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message,
                                           HttpServletRequest request) {
        return response(status, new ApiError(code, message, correlationId(request)));
    }

    private String correlationId(HttpServletRequest request) {
        return CorrelationId.of(request);
    }

    private ResponseEntity<ApiError> response(HttpStatus status, ApiError body) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body);
    }
}
