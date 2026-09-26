package com.endev.tutoring.domain;

import java.util.List;

/** Runs a list of rules, in order, on every status change. */
public final class LessonPolicy implements Rule {

    private final List<Rule> rules;

    public LessonPolicy(List<Rule> rules) {
        this.rules = List.copyOf(rules);
    }

    @Override
    public void check(LessonStatus from, LessonStatus to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Both the current and the target status are required");
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
