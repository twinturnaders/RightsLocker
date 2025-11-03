package org.rights.locker.Services;


import lombok.RequiredArgsConstructor;
import org.rights.locker.Entities.AppUser;
import org.rights.locker.Entities.Evidence;
import org.rights.locker.Entities.ShareLink;
import org.rights.locker.Repos.EvidenceRepo;
import org.rights.locker.Repos.ShareLinkRepo;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class ShareService {
    private final ShareLinkRepo repo;
    private final EvidenceRepo evidenceRepo;


    public ShareLink create(UUID evidenceId, Instant expiresAt, boolean allowOriginal){
        Evidence ev = evidenceRepo.findById(evidenceId).orElseThrow();
        var link = ShareLink.builder()
                .evidence(ev)
                .createdBy(AppUser.builder().build()) //
                .token(UUID.randomUUID().toString().replace("-", ""))
                .expiresAt(expiresAt)
                .allowOriginal(allowOriginal)
                .build();
        return repo.save(link);
    }


    public ShareLink revoke(String token){
        var link = repo.findByToken(token).orElseThrow();
        link.setRevokedAt(Instant.now());
        return repo.save(link);
    }
}