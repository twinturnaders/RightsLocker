package org.rights.locker.Repos;

import org.rights.locker.Entities.ShareLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShareLinkRepo extends JpaRepository<ShareLink, UUID> {
    Optional<ShareLink> findByToken(String token);
    boolean existsByToken(String token);
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("delete from ShareLink s where s.evidenceId = :eId")
    void deleteByEvidenceId(@org.springframework.data.repository.query.Param("eId") UUID evidenceId);
}