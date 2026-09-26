package com.endev.tutoring.handler;

import com.endev.tutoring.domain.LessonId;
import com.endev.tutoring.domain.LessonStatus;
import com.endev.tutoring.dto.VendorLessonPayload;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Runs when the app starts and shows which moves are allowed
@Component
public class LessonDemoRunner implements CommandLineRunner {

    private final LessonService lessonService;

    public LessonDemoRunner(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @Override
    public void run(String... args) {
        VendorLessonPayload payload = new VendorLessonPayload("LES-2026-0042", "PENDING");
        LessonId id = payload.toLessonId();
        LessonStatus status = payload.toLessonStatus();
        System.out.println("Lesson " + id.value() + " arrived from the vendor as " + status);

        status = tryMove(status, LessonStatus.COMPLETED);
        status = tryMove(status, LessonStatus.CONFIRMED);
        status = tryMove(status, LessonStatus.COMPLETED);
        status = tryMove(status, LessonStatus.CANCELLED);

        System.out.println("Lesson " + id.value() + " ends as " + status);
    }

    private LessonStatus tryMove(LessonStatus from, LessonStatus to) {
        try {
            lessonService.move(from, to);
            System.out.println("  " + from + " -> " + to + ": allowed");
            return to;
        } catch (IllegalStateException e) {
            System.out.println("  " + from + " -> " + to + ": forbidden (" + e.getMessage() + ")");
            return from;
        }
    }
}
