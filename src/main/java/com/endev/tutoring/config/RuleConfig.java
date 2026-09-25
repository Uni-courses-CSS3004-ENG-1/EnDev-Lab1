package com.endev.tutoring.config;

import com.endev.tutoring.domain.Rule;
import com.endev.tutoring.domain.TransitionRule;
import com.endev.tutoring.domain.UnpaidCannotComplete;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// The rules are plain Java. Spring only puts them together here, so domain never imports Spring.
@Configuration
public class RuleConfig {

    // One Rule that runs both rules, in order, on every move.
    @Bean
    public Rule lessonRules() {
        Rule unpaidCannotComplete = new UnpaidCannotComplete();
        Rule transitionRule = new TransitionRule();

        return (from, to) -> {
            unpaidCannotComplete.check(from, to);
            transitionRule.check(from, to);
        };
    }
}
