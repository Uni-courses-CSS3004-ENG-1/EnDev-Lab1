package com.endev.tutoring.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import static java.nio.charset.StandardCharsets.UTF_8;

// One HS256 key, two beans: the encoder signs at login, the decoder checks every request.
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Bean
    SecretKey jwtKey(JwtProperties jwt) {
        return key(jwt.secret());
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtKey, JwtProperties jwt) {
        return decoder(jwtKey, jwt.issuer());
    }

    public static SecretKey key(String secret) {
        return new SecretKeySpec(secret.getBytes(UTF_8), "HmacSHA256");
    }

    // Every API call: signature, expiry and iss; a token issued by another product fails here.
    // A refresh token is refused too: it only opens POST /api/v1/auth/refresh, never the API.
    public static JwtDecoder decoder(SecretKey key, String issuer) {
        return decoder(key, issuer, jwt -> TokenService.REFRESH.equals(jwt.getClaimAsString(TokenService.TYPE))
                ? OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "a refresh token", null))
                : OAuth2TokenValidatorResult.success());
    }

    // POST /api/v1/auth/refresh only: the same checks, and the token must say it is a refresh token
    public static JwtDecoder refreshDecoder(SecretKey key, String issuer) {
        return decoder(key, issuer, jwt -> TokenService.REFRESH.equals(jwt.getClaimAsString(TokenService.TYPE))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "not a refresh token", null)));
    }

    private static JwtDecoder decoder(SecretKey key, String issuer, OAuth2TokenValidator<Jwt> kind) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), kind));
        return decoder;
    }
}
