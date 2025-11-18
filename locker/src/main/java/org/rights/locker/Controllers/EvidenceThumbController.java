package org.rights.locker.Controllers;

import lombok.RequiredArgsConstructor;
import org.rights.locker.Entities.AppUser;
import org.rights.locker.Repos.EvidenceRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/evidence")
@RequiredArgsConstructor
public class EvidenceThumbController {
    private final S3Client s3;
    private final EvidenceRepo evidenceRepo;

    @Value("${app.s3.bucketHot}") private String bucketHot;

    @GetMapping(value = "/{id}/thumb", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> thumbnail(@AuthenticationPrincipal AppUser user,
            @RequestParam(required = false) UUID id,
            @RequestParam(required = false) String key
    ) throws Exception {
        String k = key;
        if (id != null) {
            var ev = evidenceRepo.findById(id).orElseThrow();
            k = ev.getThumbnailKey();
        }
        if (k == null || k.isBlank()) {
            return ResponseEntity.notFound().build();
        }

        try (ResponseInputStream<?> in = s3.getObject(GetObjectRequest.builder()
                .bucket(bucketHot).key(k).build())) {
            byte[] bytes = in.readAllBytes();
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePublic())
                    .body(bytes);
        } catch (NoSuchKeyException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
