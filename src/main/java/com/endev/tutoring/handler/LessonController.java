package com.endev.tutoring.handler;

import com.endev.tutoring.domain.Lesson;
import com.endev.tutoring.domain.LessonId;
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

// HTTP adapter for lessons. The version is in the path; the path id is the row's UUID,
// the business key stays in the body. It never touches the EntityManager.
@RestController
@RequestMapping(path = "/api/v1/lessons", produces = MediaType.APPLICATION_JSON_VALUE)
public class LessonController {

    private final LessonRegistration registration;

    public LessonController(LessonRegistration registration) {
        this.registration = registration;
    }

    // @Valid runs before this method, so a bad body never reaches the service
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LessonResponse> create(@Valid @RequestBody CreateLessonRequest request) {
        Lesson lesson = registration.create(new LessonId(request.businessKey()), request.subject());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(lesson.id())
                .toUri();
        return ResponseEntity.created(location).body(LessonResponse.from(lesson));
    }

    // a read: no register, no move, no flush
    @GetMapping("/{id}")
    public LessonResponse get(@PathVariable UUID id) {
        return LessonResponse.from(registration.find(id));
    }
}
