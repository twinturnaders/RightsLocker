package org.rights.locker.Services;

import lombok.RequiredArgsConstructor;
import org.rights.locker.Entities.ProcessingJob;
import org.rights.locker.Enums.JobStatus;
import org.rights.locker.Repos.ProcessingJobRepo;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessorService {
    private final AmqpTemplate amqp;
    private final ProcessingJobRepo jobs;
    public record JobMessage(UUID jobId, String type) {}

    public void publish(ProcessingJob job) {
        amqp.convertAndSend(
                org.rights.locker.Config.RabbitConfig.EXCHANGE,
                org.rights.locker.Config.RabbitConfig.ROUTING,
                new JobMessage(job.getId(), job.getType().name())
        );
    }

    public void complete(java.util.UUID jobId, JobStatus status, String error, String outputsJson){
        var job = jobs.findById(jobId).orElseThrow();
        job.setStatus(status);
        job.setErrorMsg(error);
        job.setPayloadJson(outputsJson);
        jobs.save(job);
    }
}