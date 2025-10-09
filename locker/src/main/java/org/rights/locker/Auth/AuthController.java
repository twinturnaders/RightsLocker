package org.rights.locker.Auth;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rights.locker.DTOs.LoginRequest;
import org.rights.locker.DTOs.RegisterRequest;
import org.rights.locker.DTOs.TokenResponse;
import org.rights.locker.Services.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;


    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req){
        var u = authService.register(req.email(), req.password(), req.displayName());
        return ResponseEntity.ok().body(new Object(){ public final String id = u.getId().toString(); });
    }


    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req){
        return authService.login(req.email(), req.password());
    }


    @PostMapping("/refresh")
    public TokenResponse refresh(@RequestBody String refreshToken){
// TODO real refresh
        return new TokenResponse("dev-access-token", "dev-refresh-token");
    }
}