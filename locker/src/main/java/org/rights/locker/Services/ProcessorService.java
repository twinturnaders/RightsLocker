package org.rights.locker.Services;

import lombok.RequiredArgsConstructor;
import org.rights.locker.Entities.ProcessingJob;
import org.rights.locker.Enums.JobStatus;
import org.rights.locker.Repos.ProcessingJobRepo;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class ProcessorService {
    private final ProcessingJobRepo jobs;


    public ProcessingJob claimNext(){
// TODO: SELECT FOR UPDATE SKIP LOCKED pattern
        return null;
    }


    public void complete(java.util.UUID jobId, JobStatus status, String error, String outputsJson){
        var job = jobs.findById(jobId).orElseThrow();
        job.setStatus(status);
        job.setErrorMsg(error);
        job.setPayloadJson(outputsJson);
        jobs.save(job);
    }
}