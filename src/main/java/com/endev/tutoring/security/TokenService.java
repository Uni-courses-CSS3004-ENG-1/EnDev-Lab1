package com.endev.tutoring.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import javax.crypto.SecretKey;

// Writes the pass: who issued it, who it is for, until when, and the roles. Then HS256 signs it.
// Anyone can read the payload, so it holds no email, no phone and no password.
@Service
public class TokenService {

    public static final Duration LIFETIME = Duration.ofMinutes(15);
    // the refresh token keeps a person signed in across page loads; it lives in an httpOnly cookie
    public static final Duration REFRESH_LIFETIME = Duration.ofDays(14);
    public static final String TYPE = "typ";
    public static final String REFRESH = "refresh";
    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtEncoder encoder;
    private final JwtDecoder refreshDecoder;
    private final String issuer;

    public TokenService(JwtEncoder encoder, SecretKey jwtKey, JwtProperties jwt) {
        this.encoder = encoder;
        this.refreshDecoder = JwtConfig.refreshDecoder(jwtKey, jwt.issuer());
        this.issuer = jwt.issuer();
    }

    // Who, and until when; no roles. On refresh the roles are read again from the account,
    // so a 14-day token never carries a role the account no longer has.
    public String issueRefresh(String username) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(username)
                .issuedAt(now)
                .expiresAt(now.plus(REFRESH_LIFETIME))
                .claim(TYPE, REFRESH)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    // the username inside a valid refresh token, or empty if it is forged, expired or not a refresh token
    public Optional<String> refreshSubject(String token) {
        try {
            return Optional.of(refreshDecoder.decode(token).getSubject());
        } catch (JwtException invalid) {
            return Optional.empty();
        }
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
