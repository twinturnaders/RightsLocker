package org.rights.locker.Services;


import lombok.RequiredArgsConstructor;
import org.rights.locker.DTOs.TokenResponse;
import org.rights.locker.Entities.AppUser;
import org.rights.locker.Enums.Role;
import org.rights.locker.Repos.AppUserRepo;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

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
                .role(Role.valueOf("USER"))
                .build();
        return users.save(user);
    }


    // TODO: implement real JWT issuance & refresh persistence
    public TokenResponse login(String email, String password) {
        var u = users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!encoder.matches(password, u.getPasswordHash())) throw new IllegalArgumentException("Invalid credentials");
        String fakeAccess = "dev-access-token";
        String fakeRefresh = "dev-refresh-token";
        return new TokenResponse(fakeAccess, fakeRefresh);
    }
}