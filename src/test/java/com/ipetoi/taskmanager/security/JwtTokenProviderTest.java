package com.ipetoi.taskmanager.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;


class JwtTokenProviderTest {

    private static final String SECRET = "integration-test-jwt-secret-key-202605";
    private static final long EXPIRATION_MS = 60_000;

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(SECRET, EXPIRATION_MS);
    }

    @Test
    void createTokenShouldReturnNonEmptyString() {
        String token = tokenProvider.createToken("peto");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void getUsernameShouldReturnOriginalUsername() {
        String token = tokenProvider.createToken("peto");
        String extracted = tokenProvider.getUsername(token);
        assertEquals("peto", extracted);
    }

    @Test
    void validateTokenShouldReturnTrueForValidToken() {
        String token = tokenProvider.createToken("peto");
        assertTrue(tokenProvider.validateToken(token));
    }

    @Test
    void validateTokenShouldReturnFalseForExpiredToken() throws InterruptedException {
        // Token provider with a 1 ms expiration time
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(SECRET, 1);
        String token = shortLivedProvider.createToken("peto");
        Thread.sleep(10); // Wait to ensure the token has expired
        assertFalse(shortLivedProvider.validateToken(token));
    }

    @Test
    void validateTokenShouldReturnFalseForTamperedToken() {
        String token = tokenProvider.createToken("peto");
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + ".invalidSignature";
        assertFalse(tokenProvider.validateToken(tampered));
    }

    @Test
    void validateTokenShouldReturnFalseForTokenSignedWithDifferentKey() {
        JwtTokenProvider otherProvider = new JwtTokenProvider("different-test-jwt-secret-key-202605", EXPIRATION_MS);
        String foreignToken = otherProvider.createToken("peto");
        assertFalse(tokenProvider.validateToken(foreignToken));
    }

    @Test
    void validateTokenShouldReturnFalseForEmptyString() {
        assertFalse(tokenProvider.validateToken(""));
    }

    @Test
    void validateTokenShouldReturnFalseForRandomString() {
        assertFalse(tokenProvider.validateToken("ez.nem.jwt"));
    }

    @Test
    void createTokenShouldEmbedCorrectExpiration() {
        long beforeMs = System.currentTimeMillis();
        String token = tokenProvider.createToken("peto");

        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date expiration = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getExpiration();

        long expirationMs = expiration.getTime();
        long tolerance = 2000;

        assertTrue(expirationMs >= beforeMs + EXPIRATION_MS - tolerance);
        assertTrue(expirationMs <= beforeMs + EXPIRATION_MS + tolerance);
    }
}