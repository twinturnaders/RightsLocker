package org.rights.locker.Security;

import java.time.Instant;
import java.util.Date;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtProps props;
    private final byte[] secretKey; // cached, derived once

    public JwtService(JwtProps props) {
        this.props = props;
        // ① Derive a stable MAC key from the configured secret once
        this.secretKey = props.secret().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public String issueAccessToken(String subject) {
        // ② Compute now/exp using configured TTL
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.accessTtlSeconds());

        // ③ Build a compact JWT signed with HS256
        return Jwts.builder()
                .subject(subject)
                .issuer(props.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(Keys.hmacShaKeyFor(secretKey))
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
                .signWith(Keys.hmacShaKeyFor(secretKey))
                .compact();
    }
}
