package com.endev.tutoring.domain;

import java.util.List;

public class LessonPolicy implements Rule {

    @Autowired
    private  List<Rule> rules;

    public LessonPolicy(List<Rule> rules) {
        this.rules = rules;
    }

    @Override
    public void check(LessonStatus from, LessonStatus to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Status must not be null");
        }
        for (Rule rule : rules) {
            rule.check(from, to);
        }
    }

    public LessonStatus move(LessonStatus from, LessonStatus to) {
        check(from, to);
        return to;
    }
}
