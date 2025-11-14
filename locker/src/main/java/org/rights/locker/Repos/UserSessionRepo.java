package org.rights.locker.Repos;


import org.rights.locker.Entities.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;


public interface UserSessionRepo extends JpaRepository<UserSession, UUID> {
    UserSession findByJwtId(String jwtToken);
}