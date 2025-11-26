package org.rights.locker.Services;

import lombok.RequiredArgsConstructor;
import org.rights.locker.Entities.AppUser;
import org.rights.locker.Repos.AppUserRepo;
import org.rights.locker.Security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserPrincipalService {

    private final AppUserRepo userRepo;

   
    public AppUser requireUser(UserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return userRepo.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    public AppUser getUserOrNull(UserPrincipal principal) {
        if (principal == null) return null;
        return userRepo.findById(principal.getId()).orElse(null);
    }
}
