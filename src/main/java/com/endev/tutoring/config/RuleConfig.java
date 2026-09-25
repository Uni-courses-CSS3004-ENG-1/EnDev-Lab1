package com.endev.tutoring.config;

import com.endev.tutoring.domain.Rule;
import com.endev.tutoring.domain.TransitionRule;
import com.endev.tutoring.domain.UnpaidCannotComplete;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

// The rules stay plain Java; Spring only composes them here, so domain never imports Spring.
@Configuration
public class RuleConfig {

    @Bean
    public Rule lessonRules() {
        List<Rule> chain = List.of(new UnpaidCannotComplete(), new TransitionRule());
        return (from, to) -> chain.forEach(rule -> rule.check(from, to));
    }
}
