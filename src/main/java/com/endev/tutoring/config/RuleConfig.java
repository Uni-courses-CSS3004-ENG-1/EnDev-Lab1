package com.endev.tutoring.config;

import com.endev.tutoring.domain.LessonPolicy;
import com.endev.tutoring.domain.Rule;
import com.endev.tutoring.domain.TransitionRule;
import com.endev.tutoring.domain.UnpaidCannotComplete;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

// The rules are plain Java. Spring only puts them together here, so domain never imports Spring.
@Configuration
public class RuleConfig {

    // The stop-factor runs first, so an unpaid lesson gets the more specific message.
    @Bean
    public Rule lessonRules() {
        return new LessonPolicy(List.of(new UnpaidCannotComplete(), new TransitionRule()));
    }
}
