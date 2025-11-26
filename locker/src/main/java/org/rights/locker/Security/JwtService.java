package org.rights.locker.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.rights.locker.Entities.AppUser;
import org.rights.locker.Repos.UserSessionRepo;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
@Getter
public class JwtService {

    private final JwtProps props;
    private final SecretKey key;
    private final UserSessionRepo sessionRepo;

    public JwtService(JwtProps props, UserSessionRepo sessionRepo) {
        this.props = props;
        // HS256 wants at least 256-bit secret; ensure your secret is long enough
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
        this.sessionRepo = sessionRepo;
    }

    public String issueAccessToken(String subject) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.accessTtlSeconds());
        return Jwts.builder()
                .subject(subject)           // subject = userId as String (UUID)
                .issuer(props.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public String issueRefreshToken(String subject) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.refreshTtlSeconds());
        return Jwts.builder()
                .subject(subject)
                .issuer(props.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    // ===== New helpers for the filter =====

    /** Low-level: parse claims, verifying signature & expiration. */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)      // validates signature
                .build()
                .parseSignedClaims(token)
                .getPayload();        // throws JwtException if invalid/expired
    }

    /** Parse + validate signature + exp; return the subject (userId UUID). */
    public String validateAndGetSubject(String token) {
        return parseClaims(token).getSubject();
    }

    /** Used by JwtAuthenticationFilter: extract userId as UUID from token. */
    public UUID extractUserId(String token) {
        try {
            String sub = validateAndGetSubject(token);
            return UUID.fromString(sub);
        } catch (IllegalArgumentException e) {
            // subject is not a valid UUID
            return null;
        } catch (JwtException e) {
            // invalid/expired token
            return null;
        }
    }

    /**
     * Used by JwtAuthenticationFilter: check that the token is valid for this user.
     * - Signature and expiration already enforced via parseClaims
     * - Subject must match user's id
     * - You can optionally add sessionRepo checks here if you do revocation.
     */
    public boolean isTokenValid(String token, AppUser user) {
        if (user == null) return false;

        try {
            Claims claims = parseClaims(token);

            // Subject must match user id
            String sub = claims.getSubject();
            if (sub == null || !sub.equals(user.getId().toString())) {
                return false;
            }

            // Extra defensive expiration check (parseClaims already enforces this)
            Date exp = claims.getExpiration();
            if (exp != null && exp.toInstant().isBefore(Instant.now())) {
                return false;
            }

            // OPTIONAL: if you have per-session/jti logic, hook sessionRepo in here.
            // e.g., check that the session id / jti in claims is not revoked.

            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Bad signature, malformed token, expired, etc.
            return false;
        }
    }
}
