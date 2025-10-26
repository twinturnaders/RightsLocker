package org.rights.locker.Services;

import lombok.RequiredArgsConstructor;
import org.rights.locker.DTOs.CurrentUserDTO;
import org.rights.locker.Entities.AppUser;
import org.rights.locker.Enums.Role;
import org.rights.locker.Repos.AppUserRepo;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
@Service

public class CurrentUserService {
    private final AppUserRepo userRepo;
    private String email;
    public CurrentUserService(AppUserRepo userRepo) {
        this.userRepo = userRepo;
    }

   public AppUser getCurrentUser(String id) {
       Optional<AppUser> user = userRepo.findById(UUID.fromString(id));
       return user.orElse(null);
   }
}
