package org.rights.locker.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final JwtProps props;
    private final SecretKey key;

    public JwtService(JwtProps props) {
        this.props = props;
        // HS256 wants at least 256-bit secret; ensure your secret is long enough
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String issueAccessToken(String subject) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.accessTtlSeconds());
        return Jwts.builder()
                .subject(subject)
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

    /** Parse + validate signature + exp; return the subject (userId UUID as string). */
    public String validateAndGetSubject(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)     // validates signature
                .build()
                .parseSignedClaims(token)
                .getPayload();       // throws if invalid or expired
        return claims.getSubject();
    }
}