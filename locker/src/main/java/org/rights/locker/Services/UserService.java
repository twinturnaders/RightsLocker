package org.rights.locker.Services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.rights.locker.DTOs.CurrentUserDTO;
import org.rights.locker.Entities.AppUser;
import org.rights.locker.Repos.AppUserRepo;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Getter
@Setter
public class UserService {

    private final AppUserRepo appUserRepo;
    private AppUser appUser;
    private CurrentUserDTO currentUser;

    public AppUser getAppUser(UUID Id) {
        return appUserRepo.findById(Id).orElse(null);
    }
    public CurrentUserDTO getCurrentUser(UUID Id) {
        AppUser appUser = getAppUser(Id);

        return new CurrentUserDTO(
                appUser.getId(),
                appUser.getEmail(),
                appUser.getPasswordHash()
        );
    }
}
