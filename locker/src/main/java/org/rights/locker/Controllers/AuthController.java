package org.rights.locker.Controllers;

import jakarta.validation.Valid;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;
import java.util.UUID;


import org.rights.locker.Security.JwtService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;  // your user/password checker
    private final JwtService jwtService;    // encapsulates props + JWT logic
    private final AppUserRepo userRepo;
    UserPrincipal userPrincipal;

    public AuthController(AuthService authService, JwtService jwtService, AppUserRepo userRepo, UserSessionRepo userSessionRepo) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.userRepo = userRepo;


    }
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
        // Expect "Bearer <refreshToken>"; issue a new access
        if (auth == null || !auth.startsWith("Bearer ")) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        String refresh = auth.substring(7).trim();
        String sub = jwtService.validateAndGetSubject(refresh); // throws if invalid/expired
        return ResponseEntity.ok(new TokenResponse(
                jwtService.issueAccessToken(sub),
                jwtService.issueRefreshToken(sub)
        ));
    }
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest req, @AuthenticationPrincipal AppUser up ) {
        var user = authService.login(req.email(), req.password());
        var id = UUID.fromString(authService.getIdFromEmail(req.email()));
         up = userRepo.findById(id).orElseThrow();

        UserPrincipal.create(up);
           var access = jwtService.issueAccessToken(authService.getIdFromEmail(req.email()));
        var refresh = jwtService.issueRefreshToken(authService.getIdFromEmail(req.email()));
        return ResponseEntity.ok(new TokenResponse(access, refresh));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req, @AuthenticationPrincipal AppUser u) {
        u = authService.register(req.email(), req.password(), req.displayName());
        userRepo.save(u);
        var access = jwtService.issueAccessToken(authService.getIdFromEmail(req.email()));
        var refresh = jwtService.issueRefreshToken(authService.getIdFromEmail(req.email()));
        return ResponseEntity.ok(new TokenResponse(access, refresh));
    }
}