package org.rights.locker.Repos;


import org.rights.locker.Entities.Evidence;
import org.rights.locker.Enums.EvidenceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.UUID;


public interface EvidenceRepo extends JpaRepository<Evidence, UUID> {
    Page<Evidence> findByStatus(EvidenceStatus status, Pageable pageable);
}