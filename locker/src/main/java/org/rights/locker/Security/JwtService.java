package org.rights.locker.Security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.rights.locker.Entities.AppUser;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

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

    public String parseSubject(String token) {
        String[] chunks = token.split("\\.");
        Base64.Decoder decoder = Base64.getUrlDecoder();

        String header = new String(decoder.decode(chunks[0]));
        String payload = new String(decoder.decode(chunks[1]));
        return payload;
    }
}