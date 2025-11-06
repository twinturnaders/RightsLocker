package org.rights.locker.Repos;


import org.rights.locker.Entities.AppUser;
import org.rights.locker.Entities.Evidence;
import org.rights.locker.Enums.EvidenceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


import java.lang.ScopedValue;
import java.util.Optional;
import java.util.UUID;




    public interface EvidenceRepo extends JpaRepository<Evidence, UUID> {
        Page<Evidence> findByStatus(EvidenceStatus status, Pageable pageable);
        Page<Evidence> findAllByOwner(AppUser owner, Pageable pageable);
        Optional<Evidence> findByIdAndOwner(UUID id, AppUser owner);

     
    }