package com.endev.tutoring.security;

import com.endev.tutoring.model.Account;
import com.endev.tutoring.model.SignUp;
import com.endev.tutoring.service.AccountService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

// Test code only: the application itself ships no users and no data.
// When a test boots the whole app, each missing account is registered through AccountService,
// the same path as the sign-up form, into the "test" schema (see src/test/resources/config).
@Configuration
public class TestAccountsConfig {

    public static final String STUDENT_PASSWORD = "student-dev";
    public static final String TUTOR_PASSWORD = "tutor-dev";

    static final List<SignUp> ACCOUNTS = List.of(
            new SignUp("aliya", STUDENT_PASSWORD, Account.STUDENT, "Aliya Nurlanova", "aliya@tutoring.test",
                    null, null, null),
            new SignUp("dana", TUTOR_PASSWORD, Account.TUTOR, "Dana Smith", "dana@tutoring.test",
                    "Algebra", new BigDecimal("25.00"), "Maths teacher for eight years."),
            new SignUp("timur", TUTOR_PASSWORD, Account.TUTOR, "Timur Akhmetov", "timur@tutoring.test",
                    "Physics", new BigDecimal("30.00"), "Olympiad coach."));

    @Bean
    ApplicationRunner testAccounts(AccountService accounts) {
        return args -> {
            for (SignUp account : ACCOUNTS) {
                if (accounts.find(account.username()).isEmpty()) {
                    accounts.register(account);
                }
            }
        };
    }
}
