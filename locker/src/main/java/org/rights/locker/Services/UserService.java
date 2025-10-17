package org.rights.locker.Services;

import org.rights.locker.Entities.AppUser;
import org.rights.locker.Repos.AppUserRepo;

import java.util.Optional;

public class UserService {
    private final AppUserRepo userRepo;
    public UserService(AppUserRepo userRepo) {
        this.userRepo = userRepo;
    }
    public Optional<AppUser> getUserByEmail(String email) {
        return userRepo.findByEmail(email);
    }

}
