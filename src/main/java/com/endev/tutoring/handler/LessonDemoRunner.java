package com.endev.tutoring.handler;

import com.endev.tutoring.domain.LessonId;
import com.endev.tutoring.domain.LessonStatus;
import com.endev.tutoring.dto.VendorLessonPayload;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import static com.endev.tutoring.domain.LessonStatus.CANCELLED;
import static com.endev.tutoring.domain.LessonStatus.COMPLETED;
import static com.endev.tutoring.domain.LessonStatus.CONFIRMED;

/** Runs on start-up so mvn spring-boot:run shows the lesson process at work. */
@Component
public class LessonDemoRunner implements CommandLineRunner {

    private final LessonService service;

    public LessonDemoRunner(LessonService service) {
        this.service = service;
    }

    @Override
    public void run(String... args) {
        VendorLessonPayload payload = new VendorLessonPayload("LES-2026-0042", "PENDING");
        LessonId id = payload.toLessonId();
        LessonStatus status = payload.toLessonStatus();
        System.out.println("Lesson " + id.value() + " arrived from the vendor as " + status);

        status = tryMove(status, COMPLETED);
        status = tryMove(status, CONFIRMED);
        status = tryMove(status, COMPLETED);
        status = tryMove(status, CANCELLED);

        System.out.println("Lesson " + id.value() + " ends as " + status);
    }

    private LessonStatus tryMove(LessonStatus from, LessonStatus to) {
        try {
            LessonStatus result = service.move(from, to);
            System.out.println("  " + from + " -> " + to + ": allowed");
            return result;
        } catch (IllegalStateException e) {
            System.out.println("  " + from + " -> " + to + ": forbidden (" + e.getMessage() + ")");
            return from;
        }
    }
}
