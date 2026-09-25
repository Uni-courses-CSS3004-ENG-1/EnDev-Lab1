package com.endev.tutoring.domain;

import java.util.List;

/** Runs every rule on a status change, without Spring. */
public final class LessonPolicy {

    private final List<Rule> rules;

    public LessonPolicy() {
        this(List.of(new UnpaidCannotComplete(), new TransitionRule()));
    }

    public LessonPolicy(List<Rule> rules) {
        this.rules = List.copyOf(rules);
    }

    public LessonStatus move(LessonStatus from, LessonStatus to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Both the current and the target status are required");
        }
        for (Rule rule : rules) {
            rule.check(from, to);
        }
        return to;
    }
}
