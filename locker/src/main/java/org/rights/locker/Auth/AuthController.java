package org.rights.locker.Auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rights.locker.Entities.AppUser;
import org.rights.locker.Enums.Role;
import org.rights.locker.Repos.AppUserRepo;
import org.rights.locker.Security.JwtProps;
import org.rights.locker.Security.JwtService;
import org.rights.locker.Utilities.CookieUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AppUserRepo users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final JwtProps props;

    public record RegisterRequest(String email, String password, String displayName){}
    public record LoginRequest(String email, String password){}

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req){
        var u = AppUser.builder()
                .email(req.email())
                .passwordHash(encoder.encode(req.password()))
                .displayName(req.displayName())
                .role(Role.valueOf("USER"))
                .build();
        u = users.save(u);
        return ResponseEntity.ok(Map.of("id", u.getId()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletResponse res){
        var u = users.findByEmail(req.email()).orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!encoder.matches(req.password(), u.getPasswordHash())) throw new IllegalArgumentException("Invalid credentials");
        var access = jwt.createAccessToken(u);
        var refresh = jwt.createRefreshToken(u);
        CookieUtil.setRefreshCookie(res, refresh, (int) props.getRefreshTtlSeconds());
        return ResponseEntity.ok(Map.of("accessToken", access));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest req, HttpServletResponse res){
        String token = null;
        if (req.getCookies() != null) {
            for (Cookie c : req.getCookies()){
                if ("refreshToken".equals(c.getName())) { token = c.getValue(); break; }
            }
        }
        if (token == null) return ResponseEntity.status(401).body(Map.of("error", "no refresh token"));
        var claims = jwt.parse(token).getBody();
        if (claims.getExpiration().toInstant().isBefore(java.time.Instant.now()))
            return ResponseEntity.status(401).body(Map.of("error", "expired refresh"));
        var email = claims.getSubject();
        var u = users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("user missing"));
        var newAccess = jwt.createAccessToken(u);
        var newRefresh = jwt.createRefreshToken(u); // rotate
        CookieUtil.setRefreshCookie(res, newRefresh, (int) props.getRefreshTtlSeconds());
        return ResponseEntity.ok(Map.of("accessToken", newAccess));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse res){
        CookieUtil.clearRefreshCookie(res);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
