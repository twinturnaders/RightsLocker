package org.rights.locker.Utilities;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.rights.locker.Entities.Evidence;
import org.rights.locker.Entities.ProcessingJob;
import org.rights.locker.Enums.JobStatus;
import org.rights.locker.Enums.JobType;
import org.rights.locker.Repos.EvidenceRepo;
import org.rights.locker.Repos.ProcessingJobRepo;
import org.rights.locker.Services.ProcessorService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobListener {

    private final ProcessingJobRepo jobs;
    private final EvidenceRepo evidenceRepo;
    private final S3Client s3;
    private final S3Presigner presigner;

    @Value("${app.s3.bucketOriginals}") private String bucketOriginals;
    @Value("${app.s3.bucketHot}") private String bucketHot;

    @RabbitListener(queues = "rl.jobs")
    public void onJob(ProcessorService.JobMessage msg) {
        var job = jobs.findById(msg.jobId()).orElse(null);
        if (job == null) return;
        try {
            switch (job.getType()) {
                case THUMBNAIL -> runThumbnail(job);
                case REDACT -> runRedact(job);
                default -> log.warn("Unknown job type {}", job.getType());
            }
            job.setStatus(JobStatus.SUCCEEDED);
            jobs.save(job);
        } catch (Exception ex) {
            log.error("Job {} failed", job.getId(), ex);
            job.setStatus(JobStatus.ERROR);
            job.setAttempts(job.getAttempts() + 1);
            jobs.save(job);
        }
    }

    /* ----------------- THUMBNAIL ----------------- */
    private void runThumbnail(ProcessingJob job) throws Exception {
        Evidence ev = evidenceRepo.findById(job.getEvidence().getId()).orElseThrow();

        // short presigned URL to original for FFmpeg
        PresignedGetObjectRequest getUrl = presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .getObjectRequest(b -> b.bucket(bucketOriginals).key(ev.getOriginalKey()))
                .build());

        // grab a representative frame (0.5s or at 10% of duration)
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(getUrl.url().toString())) {
            grabber.start();
            double fps = Math.max(1.0, grabber.getVideoFrameRate());
            int totalFrames = Math.max(1, grabber.getLengthInFrames());
            int target = Math.min(totalFrames - 1, (int) Math.round(Math.max(1, fps * 0.5)));
            for (int i = 0; i < target; i++) grabber.grabImage();
            var frame = grabber.grabImage();
            if (frame == null) throw new IllegalStateException("No video frame found");

            // Frame -> BufferedImage -> JPEG bytes
            BufferedImage img = new Java2DFrameConverter().convert(frame);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(img, "jpg", bos);
            byte[] jpeg = bos.toByteArray();

            // upload to HOT bucket
            String key = "thumbnails/" + ev.getId() + ".jpg";
            s3.putObject(PutObjectRequest.builder()
                            .bucket(bucketHot).key(key).contentType("image/jpeg").build(),
                    RequestBody.fromBytes(jpeg));

            ev.setThumbnailKey(key);
            // if nothing else done yet, you can set status to PROCESSING/READY based on your logic
            evidenceRepo.save(ev);
            grabber.stop();
        }
    }

    /* ----------------- REDACT (placeholder copy) ----------------- */
    private void runRedact(ProcessingJob job) throws Exception {
        Evidence ev = evidenceRepo.findById(job.getEvidence().getId()).orElseThrow();

        // For now, just copy original to hot as a placeholder redacted file.
        // Later, replace with real redaction and upload result to this key.
        String key = "redacted/" + ev.getId() + ".mp4";
        s3.copyObject(CopyObjectRequest.builder()
                .sourceBucket(bucketOriginals)
                .sourceKey(ev.getOriginalKey())
                .destinationBucket(bucketHot)
                .destinationKey(key)
                .build());

        ev.setRedactedKey(key);
        // If you distinguish states: ev.setStatus(EvidenceStatus.REDACTED);
        evidenceRepo.save(ev);
    }
}
