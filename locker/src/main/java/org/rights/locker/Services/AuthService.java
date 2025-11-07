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

    public AppUser register(String email, String password, String displayName) {
        var user = AppUser.builder()
                .email(email)
                .passwordHash(encoder.encode(password))
                .displayName(displayName)
                .role(Role.USER)
                .build();
        return users.save(user);
    }

    public AppUser loginAndReturnUser(String email, String password) {
        var user = users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!encoder.matches(password, user.getPasswordHash()))
            throw new IllegalArgumentException("Invalid credentials");
        return user;
    }
}
