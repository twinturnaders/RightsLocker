package org.rights.locker.Services;

import lombok.Getter;
import org.rights.locker.Entities.AppUser;
import org.rights.locker.Repos.AppUserRepo;
import org.rights.locker.Repos.EvidenceRepo;
import org.rights.locker.Security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;
@Service

public class UserPrincipalService {
    private final AppUserRepo userRepo;
    @Getter
    private AppUser user;


    public UserPrincipalService(AppUserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public AppUser requireUser(UserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        return userRepo.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    public AppUser checkUser(UserPrincipal principal) {
        if (principal == null) {
            return null;
        } else if (userRepo.findById(principal.getId()).isPresent()) {
            return userRepo.findById(principal.getId()).get();

        }
     else return null;
    }

}
