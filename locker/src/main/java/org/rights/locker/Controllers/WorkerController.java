package org.rights.locker.Controllers;

import lombok.RequiredArgsConstructor;
import org.rights.locker.Enums.JobStatus;
import org.rights.locker.Services.ProcessorService;
import org.springframework.web.bind.annotation.*;


import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/api/processor")
@RequiredArgsConstructor
public class WorkerController {
    private final ProcessorService service;


    @PostMapping("/jobs:claim")
    public Map<String, Object> claim(){
        var job = service.claimNext();
        return job == null ? Map.of("job", null) : Map.of("jobId", job.getId(), "type", job.getType().name());
    }


    @PostMapping("/jobs/{id}/done")
    public void done(@PathVariable UUID id, @RequestBody Map<String, Object> req){
        var status = JobStatus.valueOf((String) req.getOrDefault("status", "SUCCESS"));
        var error = (String) req.getOrDefault("error", null);
        var outputs = (String) req.getOrDefault("outputs", "{}");
        service.complete(id, status, error, outputs);
    }
}