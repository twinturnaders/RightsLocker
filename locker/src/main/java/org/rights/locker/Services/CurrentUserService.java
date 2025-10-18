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
    public long getCurrentUserIdByEmail(String email) {
        return userRepo.findIdByEmail(email);
    }
    public Optional<AppUser> getUserByEmail(String email) {
        return userRepo.findByEmail(email);
    }


    public Optional<AppUser> getCurrentUserbyId(String userId) {
        return userRepo.findById(UUID.fromString(userId));
    }
    public CurrentUserDTO findCurrentUserDTOById(String userId) {
        AppUser user = userRepo.findById(UUID.fromString(userId)).orElseThrow();
        return new CurrentUserDTO(user.getId(), user.getEmail(), user.getRole().toString());
    }
    public Role getRoleByUserId(String userId) {
        AppUser user = userRepo.findById(UUID.fromString(userId)).orElseThrow();
        return user.getRole();
    }
}
