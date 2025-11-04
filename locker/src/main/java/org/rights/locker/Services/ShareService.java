

package org.rights.locker.Services;

import lombok.RequiredArgsConstructor;
import org.rights.locker.Entities.Evidence;
import org.rights.locker.Entities.ShareLink;
import org.rights.locker.Repos.EvidenceRepo;
import org.rights.locker.Repos.ShareLinkRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

    @Service
    @RequiredArgsConstructor
    public class ShareService {
        private final EvidenceRepo evidenceRepo;
        private final ShareLinkRepo repo;
        private static final SecureRandom RNG = new SecureRandom();

        public ShareLink create(UUID evidenceId, Instant expiresAt, boolean allowOriginal) {
            var exp = (expiresAt == null) ? Instant.now().plus(7, ChronoUnit.DAYS) : expiresAt;
            var token = mintUniqueToken(24); // ~128-bit url-safe token
            Evidence ev = evidenceRepo.findById(evidenceId).orElseThrow();
            var s = ShareLink.builder()
                    .token(token)
                    .evidence(ev)
                    .allowOriginal(allowOriginal)
                    .expiresAt(exp)
                    .build();
            return repo.save(s);
        }

        public ShareLink requireActive(String token) {
            var s = repo.findByToken(token)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Share not found"));
            if (s.getRevokedAt() != null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Share revoked");
            if (s.getExpiresAt() != null && !s.getExpiresAt().isAfter(Instant.now())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Share expired");
            }
            return s;
        }

        public ShareLink revoke(String token) {
            var s = repo.findByToken(token)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Share not found"));
            if (s.getRevokedAt() == null) {
                s.setRevokedAt(Instant.now());
                s = repo.save(s);
            }
            return s;
        }

        private String mintUniqueToken(int bytes) {
            // URL-safe, no padding, base64url
            for (int i = 0; i < 5; i++) {
                byte[] raw = new byte[bytes];
                RNG.nextBytes(raw);
                var t = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
                if (!repo.existsByToken(t)) return t;
            }
            // Extremely unlikely fallback
            return UUID.randomUUID().toString().replace("-", "");
        }
    }