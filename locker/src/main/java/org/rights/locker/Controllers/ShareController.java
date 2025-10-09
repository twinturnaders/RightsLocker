package org.rights.locker.Controllers;

import lombok.RequiredArgsConstructor;
import org.rights.locker.DTOs.ShareCreateRequest;
import org.rights.locker.Entities.ShareLink;
import org.rights.locker.Services.ShareService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.time.Instant;
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ShareController {
    private final ShareService shareService;


    @PostMapping("/evidence/{id}/share")
    public ShareLink create(@PathVariable UUID id, @RequestBody ShareCreateRequest req){
        return shareService.create(id, req.expiresAt(), req.allowOriginal());
    }


    @GetMapping("/share/{token}")
    public ResponseEntity<?> getShare(@PathVariable String token){
// TODO: return signed URLs / metadata for public viewer
        return ResponseEntity.ok(Map.of("token", token));
    }


    @PostMapping("/share/{token}/revoke")
    public ShareLink revoke(@PathVariable String token){
        return shareService.revoke(token);
    }
}