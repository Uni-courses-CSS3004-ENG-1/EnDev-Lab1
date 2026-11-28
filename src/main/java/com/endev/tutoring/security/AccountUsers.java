package com.endev.tutoring.security;

import com.endev.tutoring.repository.AccountRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// Login asks this class for a user by what they typed: an email (or an older username). It reads the
// app_user row (V4) and hands Spring the
// BCrypt hash and the role; Spring then compares the typed password with the hash.
@Service
public class AccountUsers implements UserDetailsService {

    private final AccountRepository accounts;

    public AccountUsers(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return accounts.findByLogin(username)
                // roles(...) stores ROLE_STUDENT or ROLE_TUTOR, which hasRole(...) looks for
                .map(row -> User.withUsername(row.getUsername())
                        .password(row.getPasswordHash())
                        .roles(row.getRole())
                        .build())
                // Spring turns this into the same BadCredentialsException as a wrong password
                .orElseThrow(() -> new UsernameNotFoundException("no such user"));
    }
}
