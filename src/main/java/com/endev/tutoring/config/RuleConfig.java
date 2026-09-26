package com.endev.tutoring.config;

import com.endev.tutoring.domain.LessonPolicy;
import com.endev.tutoring.domain.Rule;
import com.endev.tutoring.domain.TransitionRule;
import com.endev.tutoring.domain.UnpaidCannotComplete;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RuleConfig {

    @Bean
    public Rule lessonRules() {
        List<Rule> rules = List.of(new UnpaidCannotComplete(), new TransitionRule());
        return new LessonPolicy(rules);
    }
}
