package org.rights.locker.Controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rights.locker.DTOs.LoginRequest;
import org.rights.locker.DTOs.RegisterRequest;
import org.rights.locker.DTOs.TokenResponse;
import org.rights.locker.Entities.AppUser;
import org.rights.locker.Entities.UserSession;
import org.rights.locker.Enums.Role;
import org.rights.locker.Repos.AppUserRepo;
import org.rights.locker.Repos.UserSessionRepo;
import org.rights.locker.Security.CurrentUser;
import org.rights.locker.Security.UserPrincipal;
import org.rights.locker.Services.AuthService;
import org.rights.locker.Services.TokenService;
import org.rights.locker.Services.UserSessionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;


import org.rights.locker.Security.JwtService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;
    private final JwtService jwtService;
    private final AppUserRepo userRepo;
    private final UserSessionRepo userSessionRepo;
    private final UserSessionService userSessionService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest req) {
        AppUser user = authService.loginAndReturnUser(req.email(), req.password());
        String sub = user.getId().toString();
        String token = jwtService.issueAccessToken(sub);
        userSessionService.createUserSession(user, token);
         return ResponseEntity.ok(new TokenResponse(
                token,
                jwtService.issueRefreshToken(sub)
        ));



    }
    @PostMapping("/logout")
    public void logout(@RequestHeader("Authorization") String authorizationHeader) {
       String jwtToken = tokenService.getToken(authorizationHeader);
            UserSession session = userSessionRepo.findByJwtId(jwtToken);
            if (session != null) {
                session.setExpiresAt(Instant.now());
            }
        }



    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest req) {
        AppUser u = authService.register(req.email(), req.password(), req.displayName());
        String sub = u.getId().toString();
        return ResponseEntity.ok(new TokenResponse(
                jwtService.issueAccessToken(sub),
                jwtService.issueRefreshToken(sub)
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        String refresh = auth.substring(7).trim();
        String sub = jwtService.validateAndGetSubject(refresh);
        return ResponseEntity.ok(new TokenResponse(
                jwtService.issueAccessToken(sub),
                jwtService.issueRefreshToken(sub)
        ));
    }
}
