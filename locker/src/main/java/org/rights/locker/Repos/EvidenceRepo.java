package org.rights.locker.Repos;


import org.rights.locker.Entities.AppUser;
import org.rights.locker.Entities.Evidence;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;
import java.util.UUID;




    public interface EvidenceRepo extends JpaRepository<Evidence, UUID> {

        Optional<Evidence> findByIdAndOwner(UUID id, AppUser owner);





        Page<Evidence> findByOwner(AppUser user, Pageable pageable);
    }