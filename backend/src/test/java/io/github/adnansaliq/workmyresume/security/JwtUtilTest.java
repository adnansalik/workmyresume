package io.github.adnansaliq.workmyresume.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    // Mirrors the secret used by JwtUtil so tests can mint their own tokens.
    private static final String SECRET = "CHANGE_THIS_TO_A_32_CHAR_MIN_SECRET_KEY_123456";

    private final JwtUtil jwtUtil = new JwtUtil();

    private Key key() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void validTokenRoundTrip() {
        String token = jwtUtil.generateToken("user@example.com");

        assertThat(jwtUtil.isTokenValid(token)).isTrue();
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("user@example.com");
    }

    @Test
    void expiredTokenIsRejected() {
        String expired = Jwts.builder()
                .setSubject("user@example.com")
                .setIssuedAt(new Date(System.currentTimeMillis() - 120_000))
                .setExpiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();

        assertThat(jwtUtil.isTokenValid(expired)).isFalse();
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtUtil.generateToken("user@example.com");

        // Flip a character in the MIDDLE of the token. (The final base64 char is
        // avoided on purpose: its low bits can be discarded during decoding,
        // which would leave the signature bytes unchanged.)
        int mid = token.length() / 2;
        char c = token.charAt(mid);
        String tampered = token.substring(0, mid)
                + (c == 'a' ? 'b' : 'a')
                + token.substring(mid + 1);

        assertThat(tampered).isNotEqualTo(token);
        assertThat(jwtUtil.isTokenValid(tampered)).isFalse();
    }

    @Test
    void garbageTokenIsRejected() {
        assertThat(jwtUtil.isTokenValid("not-a-jwt")).isFalse();
        assertThat(jwtUtil.isTokenValid("")).isFalse();
        assertThat(jwtUtil.isTokenValid(null)).isFalse();
    }
}