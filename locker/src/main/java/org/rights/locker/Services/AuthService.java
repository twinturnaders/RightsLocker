package org.rights.locker.Services;


import lombok.RequiredArgsConstructor;
import org.apache.catalina.User;
import org.rights.locker.DTOs.TokenResponse;
import org.rights.locker.Entities.AppUser;
import org.rights.locker.Enums.Role;
import org.rights.locker.Repos.AppUserRepo;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
@RequiredArgsConstructor
public class AuthService {
    private final AppUserRepo users;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final AppUserRepo appUserRepo;


    public AppUser register(String email, String password, String displayName) {
        var user = AppUser.builder()
                .email(email)
                .passwordHash(encoder.encode(password))
                .displayName(displayName)
                .role(Role.valueOf("USER"))
                .build();
        return users.save(user);
    }



    public TokenResponse login(String email, String password) {
//        var u = users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
//
//        if (!encoder.matches(password, u.getPasswordHash())) throw new IllegalArgumentException("Invalid credentials");
//        String fakeAccess = "dev-access-token";
//        String fakeRefresh = "dev-refresh-token";
//
//        return new TokenResponse(fakeAccess, fakeRefresh);
        var user = users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!encoder.matches(password, user.getPasswordHash())) throw new IllegalArgumentException("Invalid credentials");
        var access = user.getId().toString();
        var refresh = user.getId().toString();
        var userId = user.getId().toString();
        return new TokenResponse(access, refresh, userId);
    }

 public String getIdFromEmail(String email) {
    AppUser user = users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));
    return user.getId().toString();
    }
}