package com.endev.tutoring.handler;

import com.endev.tutoring.domain.LessonStatus;
import com.endev.tutoring.domain.Rule;
import org.springframework.stereotype.Service;

@Service
public class LessonService {

    private final Rule rules;

    public LessonService(Rule rules) {
        this.rules = rules;
    }

    public LessonStatus move(LessonStatus from, LessonStatus to) {
        rules.check(from, to);
        return to;
    }
}
