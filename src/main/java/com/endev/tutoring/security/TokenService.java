package com.endev.tutoring.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

// Writes the pass: who issued it, who it is for, until when, and the roles. Then HS256 signs it.
// Anyone can read the payload, so it holds no email, no phone and no password.
@Service
public class TokenService {

    public static final Duration LIFETIME = Duration.ofMinutes(15);
    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtEncoder encoder;
    private final String issuer;

    public TokenService(JwtEncoder encoder, JwtProperties jwt) {
        this.encoder = encoder;
        this.issuer = jwt.issuer();
    }

    public String issue(Authentication user) {
        Instant now = Instant.now();
        // "ROLE_TUTOR" becomes "TUTOR"; the decoder's converter adds the prefix back
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(ROLE_PREFIX))
                .map(authority -> authority.substring(ROLE_PREFIX.length()))
                .toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getName())
                .issuedAt(now)
                .expiresAt(now.plus(LIFETIME))
                .claim("roles", roles)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
