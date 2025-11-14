package org.rights.locker.Services;


import lombok.RequiredArgsConstructor;
import org.rights.locker.Entities.AppUser;
import org.rights.locker.Entities.UserSession;
import org.rights.locker.Enums.Role;
import org.rights.locker.Repos.AppUserRepo;
import org.rights.locker.Repos.UserSessionRepo;
import org.rights.locker.Security.JwtService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class AuthService {
    private final AppUserRepo users;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final UserSessionRepo userSessionRepo;
    private final JwtService jwtService;
    UserSession userSession;

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
        var expires = Instant.now().plus(1, ChronoUnit.HOURS);
        UserSession userSession = new UserSession(null, user, jwtService.getKey().toString(), Instant.now(), expires  );
        userSessionRepo.save(userSession);
        return user;

    }

    public void logout(UUID id){
        var user = users.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));

    }
}
