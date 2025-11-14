package org.rights.locker.Services;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.rights.locker.Entities.AppUser;
import org.rights.locker.Entities.UserSession;
import org.rights.locker.Repos.AppUserRepo;
import org.rights.locker.Repos.UserSessionRepo;
import org.rights.locker.Security.UserPrincipal;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@Builder
@RequiredArgsConstructor
public class UserSessionService {
    private final UserSessionRepo userSessionRepo;
    private final AppUserRepo appUserRepo;


    public void createUserSession(AppUser appUser, String token) {
        Instant now = Instant.now();
        Instant exp = now.plus(1, ChronoUnit.HOURS);
        UserSession userSession = new UserSession(
                null,
                appUser,
                token,
                now,
                exp
        );
        userSessionRepo.save(userSession);
        UserPrincipal.create(appUser);

    }

}

