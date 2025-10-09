package org.rights.locker.Controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rights.locker.DTOs.EvidenceResponse;
import org.rights.locker.Entities.Evidence;
import org.rights.locker.Enums.EvidenceStatus;
import org.rights.locker.Repos.EvidenceRepo;
import org.rights.locker.Services.EvidenceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.util.UUID;


@RestController
@RequestMapping("/api/evidence")
@RequiredArgsConstructor
public class EvidenceController {
    private final EvidenceService service;
    private final EvidenceRepo repo;


    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "title", required = false) String title,
                                    @RequestParam(value = "description", required = false) String description,
                                    @RequestParam(value = "capturedAt", required = false) String capturedAt,
                                    @RequestParam(value = "lat", required = false) Double lat,
                                    @RequestParam(value = "lon", required = false) Double lon,
                                    @RequestParam(value = "accuracy", required = false) Double accuracy) {
        var ev = service.upload(file, title, description, capturedAt == null ? null : java.time.Instant.parse(capturedAt), lat, lon, accuracy);
        return ResponseEntity.ok(new EvidenceResponse(ev.getId(), ev.getTitle(), ev.getDescription(), ev.getCapturedAt(), ev.getStatus().name(), ev.isLegalHold(), null, ev.getThumbnailKey()));
    }


    @GetMapping
    public Page<Evidence> list(@RequestParam(value = "status", required = false) EvidenceStatus status,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size){
        return service.list(status, PageRequest.of(page, size));
    }


    @GetMapping("/{id}")
    public Evidence get(@PathVariable UUID id){ return repo.findById(id).orElseThrow(); }


    @PostMapping("/{id}/legal-hold")
    public Evidence legalHold(@PathVariable UUID id, @RequestBody java.util.Map<String, Boolean> body){
        var ev = repo.findById(id).orElseThrow();
        ev.setLegalHold(Boolean.TRUE.equals(body.get("legalHold")));
        return repo.save(ev);
    }
}