package com.endev.tutoring.service;

import com.endev.tutoring.entity.AccountEntity;
import com.endev.tutoring.exception.AccountNotFoundException;
import com.endev.tutoring.exception.DuplicateUsernameException;
import com.endev.tutoring.exception.InvalidSignUpException;
import com.endev.tutoring.model.Account;
import com.endev.tutoring.model.Profile;
import com.endev.tutoring.model.SignUp;
import com.endev.tutoring.model.Student;
import com.endev.tutoring.model.Tutor;
import com.endev.tutoring.repository.AccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

// Sign-up and "who am I" for the app_user table.
// One sign-up is one transaction: the student or tutor row and the account row are written together,
// or, if anything fails (a taken email, a taken username), neither is.
@Service
public class AccountService {

    private final AccountRepository accounts;
    private final StudentService students;
    private final TutorService tutors;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AccountRepository accounts, StudentService students, TutorService tutors,
                          PasswordEncoder passwordEncoder) {
        this.accounts = accounts;
        this.students = students;
        this.tutors = tutors;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Account register(SignUp form) {
        // people sign up with an email; it doubles as the username unless one was given
        String email = form.email() == null ? null : form.email().trim().toLowerCase(Locale.ROOT);
        String username = form.username() == null || form.username().isBlank() ? email : form.username();
        SignUp signUp = new SignUp(username, form.password(), form.role(), form.name(), email,
                form.subject(), form.hourlyRate(), form.bio());
        if (accounts.findByUsername(signUp.username()).isPresent()) {
            throw new DuplicateUsernameException(signUp.username());
        }

        // 1. the profile row: a tutor needs a subject and a price, a student only a name and email
        UUID studentId = null;
        UUID tutorId = null;
        if (Account.TUTOR.equals(signUp.role())) {
            if (signUp.subject() == null || signUp.subject().isBlank()) {
                throw new InvalidSignUpException("subject is required for a tutor");
            }
            if (signUp.hourlyRate() == null) {
                throw new InvalidSignUpException("hourlyRate is required for a tutor");
            }
            tutorId = tutors.create(signUp.name(), signUp.email(), signUp.subject(), signUp.hourlyRate(),
                    signUp.bio()).id();
        } else {
            studentId = students.create(signUp.name(), signUp.email()).id();
        }

        // 2. the login row, with the password BCrypt-hashed: {bcrypt}$2a$10$...
        AccountEntity row = new AccountEntity();
        row.setId(UUID.randomUUID());
        row.setUsername(signUp.username());
        row.setPasswordHash(passwordEncoder.encode(signUp.password()));
        row.setRole(signUp.role());
        row.setStudentId(studentId);
        row.setTutorId(tutorId);
        try {
            accounts.insert(row);
            // flush inside the try, so two sign-ups racing for one name end here and not at commit
            accounts.flush();
        } catch (DataIntegrityViolationException e) {
            if (UniqueViolation.is(e)) {
                throw new DuplicateUsernameException(signUp.username());
            }
            throw e;
        }
        return toAccount(row);
    }

    @Transactional(readOnly = true)
    public Optional<Account> find(String username) {
        return accounts.findByUsername(username).map(AccountService::toAccount);
    }

    @Transactional(readOnly = true)
    public Account require(String username) {
        return find(username).orElseThrow(() -> new AccountNotFoundException(username));
    }

    // the account plus the name and email from its student or tutor row
    @Transactional(readOnly = true)
    public Profile profile(String username) {
        Account account = require(username);
        if (account.isTutor()) {
            Tutor tutor = tutors.find(account.tutorId());
            return new Profile(account, tutor.name(), tutor.email(), tutor);
        }
        Student student = students.find(account.studentId());
        return new Profile(account, student.name(), student.email(), null);
    }

    private static Account toAccount(AccountEntity row) {
        return new Account(row.getId(), row.getUsername(), row.getRole(), row.getStudentId(), row.getTutorId());
    }
}
