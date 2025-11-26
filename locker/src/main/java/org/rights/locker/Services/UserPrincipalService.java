package org.rights.locker.Services;

import lombok.RequiredArgsConstructor;
import org.rights.locker.Entities.AppUser;
import org.rights.locker.Repos.AppUserRepo;
import org.rights.locker.Security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserPrincipalService {

    private final AppUserRepo userRepo;

    /** Require an authenticated user; otherwise 401. */
    public AppUser requireUser(UserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication");
        }
        UUID id = principal.getId();
        return userRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    /** Optional auth – returns null if no/invalid principal. */
    public AppUser getUserOrNull(UserPrincipal principal) {
        if (principal == null) return null;
        return userRepo.findById(principal.getId()).orElse(null);
    }
}
