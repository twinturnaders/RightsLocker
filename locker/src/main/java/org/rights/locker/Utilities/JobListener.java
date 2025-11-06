package org.rights.locker.Utilities;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.rights.locker.Config.RabbitConfig;
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
    private final ProcessorService processor;

    @Value("${app.s3.bucketOriginals}") private String bucketOriginals;
    @Value("${app.s3.bucketHot}") private String bucketHot;

    @RabbitListener(queues = RabbitConfig.JOBS_QUEUE)
    public void onJob(ProcessorService.JobMessage msg) {
        log.info("Got job {} type={} attempt={}", msg.jobId(), msg.type(), msg.attempt());
        var job = processor.markStarted(msg.jobId());
        try {
            switch (msg.type()) {
                case THUMBNAIL -> runThumbnail(job);
                case REDACT    -> runRedact(job); // will read job payload
                default        -> Thread.sleep(250);
            }


            String outputKey = switch (msg.type()) {
                case THUMBNAIL -> "thumbnails/%s.jpg".formatted(msg.evidenceId());
                case REDACT    -> "redacted/%s.mp4".formatted(msg.evidenceId());
                default        -> "out/%s.bin".formatted(msg.evidenceId());
            };

            // processor.completeSuccess(job.getId(), outputKey, size, sha)
            processor.completeSuccess(job.getId(), outputKey, 0L, null);
        } catch (Exception ex) {
            processor.completeFailure(job.getId(), ex.getMessage());
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
        var ev = evidenceRepo.findById(job.getEvidence().getId()).orElseThrow();
        String mode = job.getPayloadJson() != null
                ? String.valueOf(job.getPayloadJson().getOrDefault("mode","BLUR")).toUpperCase()
                : "BLUR";

        // TODO: implement real blur; placeholder: copy original → hot
        String key = "redacted/" + ev.getId() + ".mp4";
        s3.copyObject(cb -> cb
                .sourceBucket(bucketOriginals).sourceKey(ev.getOriginalKey())
                .destinationBucket(bucketHot).destinationKey(key));

        processor.completeSuccess(job.getId(), key, null, null);
    }
}
